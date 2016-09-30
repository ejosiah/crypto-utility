package client

import javax.crypto.KeyGenerator
import javax.inject.Inject

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import client.Client.{GetOutcome, StartStreaming, UserIsOffline, StreamingResult}
import com.cryptoutility.protocol.Events.Event
import play.api.Configuration
import akka.pattern.ask
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.language.postfixOps
/**
  * Created by jay on 30/09/2016.
  */
trait ClientService {

  def startStreaming(clientId: String, filename: String, sender: String, contentType: Option[String] )
  : (ActorRef, Future[Future[StreamingResult]])

  def initializeClient: Flow[Event, Event, _]

  def validateClient(id: String): Future[_]
}

class DefaultClientService @Inject()(config: Configuration)
                     (implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext) extends ClientService{

  lazy val algorithm = config.getString("cipher.symmetric.key.algorithm").get
  val registry = system.actorOf(Client.registry, "client-registry")
  implicit val timeout = Timeout(5 seconds)

  def startStreaming(clientId: String, filename: String, sender: String, contentType: Option[String] = None)
    : (ActorRef, Future[Future[StreamingResult]]) = {
    val client = Await.result(findClientById(clientId), timeout.duration)
    client ! StartStreaming(filename, contentType.getOrElse(""), sender, nextSecret)
    val streamingResult =  (client ? GetOutcome).mapTo[Future[StreamingResult]]
    (client, streamingResult)
  }

  def validateClient(id: String): Future[_] = ???

  def initializeClient: Flow[Event, Event, _] = Client.flow(registry, config)

  def findClientById(id: String): Future[ActorRef] = {
    (registry ? Client.Get(id)).map{
        case ref: ActorRef => ref
        case UserIsOffline => throw new RuntimeException(s"client with id $id is currently offline")
      }
  }

  def nextSecret = KeyGenerator.getInstance(algorithm).generateKey()
}
