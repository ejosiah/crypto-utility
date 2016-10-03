package client

import java.security.Key

import akka.Done
import akka.actor._
import akka.pattern.pipe
import akka.stream.scaladsl.Flow
import akka.stream.{ActorMaterializer, Materializer, OverflowStrategy}
import akka.util.ByteString
import client.Client.{EndOfStream, GetOutcome, StartStreaming, StreamingResult}
import com.cryptoutility.protocol.Events._
import play.api.libs.streams.ActorFlow
import play.api.{Configuration, Logger}
import streams.Crypto

import scala.collection.mutable
import scala.concurrent.Promise
import scala.language.postfixOps
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

object Client{

  val bufferSize = Int.MaxValue

  case class Online(user: UserInfo)
  case class OffLine(user: UserInfo)
  case class Get(clientId: String)
  case object EndOfStream
  case object GetOutcome
  case object UserIsOffline
  case class StartStreaming(filename: String, contentType: String, from: String, secret: Key)
  case class StreamingResult(count: Long, status: Try[Done]){

    def wasSuccessful = status.isSuccess

    def getError = status match {
      case Failure(NonFatal(e)) => e
      case Success(_) => throw new UnsupportedOperationException("streaming was successful")
    }
  }

  def props(registry: ActorRef, config: Configuration)(out: ActorRef) = Props(classOf[Client], out, registry, config)

  def registry = Props(classOf[ClientRegistry])

  def flow(registry: ActorRef, config: Configuration)(implicit factory: ActorRefFactory, mat: Materializer): Flow[Event, Event, _] = {
    ActorFlow.actorRef(props(registry, config), bufferSize, overflowStrategy = OverflowStrategy.fail)
  }

  def streamProcessor(out: ActorRef, clientId: String
                      , encryptEncoded: (Array[Byte], Key) => String
                      , wrap: Key => String): Props =
    Props(classOf[StreamProcessor], out, clientId, encryptEncoded, wrap)
}

class Client(out: ActorRef, registry: ActorRef, config: Configuration) extends Actor{
  import Client._
  implicit val ec = context.system.dispatcher
  var mayBeUser = Option.empty[UserInfo]
  var mayBeProcessor = Option.empty[ActorRef]

  override def preStart = Logger.info(s"$self connected to server")

  override def receive = {
    case Initialized(isNew, user) =>
      mayBeUser = Some(user)
      registry ! Online(user)
      out ! UserCreated(user)
    case start: StartStreaming =>
      if(mayBeProcessor.isEmpty){
        val pubKey = mayBeUser.get.key
        val clientId = mayBeUser.get.clientId
        val encryptCtx = new EncryptionContext(pubKey, config)
        import encryptCtx._

        val processor = context.actorOf(streamProcessor(out, clientId, encryptEncoded, wrap))
        context.watch(processor)
        processor ! start
        mayBeProcessor = Some(processor)
      }else{
        // TODO illegal start, how do we fix this
      }
    case chunk: ByteString =>
      mayBeProcessor.foreach(_ ! chunk)
    case Terminated(ref) =>
      if(mayBeProcessor.contains(ref))
        mayBeProcessor = None
    case msg @ GetOutcome => mayBeProcessor.foreach(_ forward msg)
    case msg @ EndOfStream => mayBeProcessor.foreach(_ forward msg)
  }


  override def postStop(): Unit = {
    mayBeUser.foreach(registry ! _)
    Logger.info(s"$self disconnected from server")
  }
}

class ClientRegistry extends Actor{
  import Client._

  var users = mutable.Map[String,  (UserInfo, ActorRef)]()
  var refs = Set[ActorRef]()

  def receive = {
    case Online(user) =>
      users += user.clientId -> (user -> sender)
      context.watch(sender)
      Logger.info(s"user(${user.clientId}) registered")
    case OffLine(user) =>
      users -= user.clientId
    case Get(clientId) =>
     users.get(clientId) match {
       case Some(user) => sender ! user
       case None => sender ! UserIsOffline
     }
    case Terminated(ref) =>
      users.find(e => e._2._2 == ref).map(_._2._1)
        .foreach( user => self ! OffLine(user))
  }

}


class StreamProcessor(out: ActorRef, clientId: String
                      , encrypt: (Array[Byte], Key) => String
                      , wrap: Key => String) extends Actor{
  var processed = 0
  var read = 0
  val promise = Promise[StreamingResult]()
  val (digester, digest) = Crypto.actorDigester("MD5")(context.system, ActorMaterializer())
  implicit val ec = context.system.dispatcher

  implicit def strToBytes(str: String): Array[Byte] = str.getBytes()


  override def preStart(): Unit = Logger.info(s"streaming started for client: $clientId")

  def receive = {
    case StartStreaming(f, ct, fr, secret) =>
      out ! StreamStarted(
          encrypt(f, secret)
        , encrypt(ct, secret)
        , encrypt(fr, secret)
        , wrap(secret))
    case chunk: ByteString =>
      out ! new StreamPart(processed, chunk.toArray)
      digester ! chunk
      processed = processed + 1
    case eos @ EndOfStream =>
      Logger.info(s"stream ended processed $processed chunks")
      digester ! eos
      val end =  digest.map(StreamEnded(processed, _))
      pipe(end) to  out
      promise.success(StreamingResult(processed, Try(Done)))
      self ! PoisonPill
    case GetOutcome => sender ! promise.future
  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    // send message out client about failure
    promise.success(StreamingResult(processed, Failure(reason)))
  }
}