package client

import java.security.Key

import akka.Done
import akka.actor._
import akka.stream.scaladsl.Flow
import akka.stream.{ActorMaterializer, Materializer, OverflowStrategy}
import akka.util.ByteString
import akka.pattern.pipe
import client.Client.{GetOutcome, StreamingResult, EndOfStream, StartStreaming}
import com.cryptoutility.protocol.Events._
import play.api.{Configuration, Logger}
import play.api.libs.streams.ActorFlow
import streams.Crypto
import scala.concurrent.duration._

import scala.collection.mutable
import scala.concurrent.{Await, Promise}
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
                      , encrypt: (Array[Byte], Key) => Array[Byte]
                      , encryptEncoded: (Array[Byte], Key) => String
                      , wrap: Key => String): Props =
    Props(classOf[StreamProcessor], out, clientId, encrypt, encryptEncoded, wrap)
}

class Client(out: ActorRef, registry: ActorRef, config: Configuration) extends Actor with ActorLogging{
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

        val processor = context.actorOf(streamProcessor(out, clientId, encrypt, encryptEncoded, wrap))
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
       case Some((_, ref)) => sender ! ref
       case None => sender ! UserIsOffline
     }
    case Terminated(ref) =>
      users.find(e => e._2._2 == ref).map(_._2._1)
        .foreach( user => self ! OffLine(user))

  }
}

class StreamProcessor(out: ActorRef, clientId: String
                      , encrypt: (Array[Byte], Key) => Array[Byte]
                      , encrypt0: (Array[Byte], Key) => String
                      , wrap: Key => String) extends Actor with ActorLogging{
  var mayBeSecret = Option.empty[Key]
  var processed = 0
  val promise = Promise[StreamingResult]()
  val (digester, digest) = Crypto.actorDigester("MD5")(context.system, ActorMaterializer())
  implicit val ec = context.system.dispatcher


  override def preStart(): Unit = log.info(s"streaming started for client: $clientId")

  def receive = {
    case StartStreaming(f, ct, fr, sec) =>
      mayBeSecret = Option(sec)
      out ! StreamStarted(
          encrypt0(f.getBytes, sec)
        , encrypt0(ct.getBytes, sec)
        , encrypt0(fr.getBytes, sec)
        , wrap(sec))
    case chunk: ByteString =>
      val secret = mayBeSecret.get
      val data =  encrypt(chunk.toArray, secret)
      out ! new StreamPart(processed, data)
      digester ! ByteString(data)
      processed = processed + 1
      println(s"processed $processed chunks")
    case eos @ EndOfStream =>
      println(s"stream ended processed $processed chunks")
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