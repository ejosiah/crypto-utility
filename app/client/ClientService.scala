package client

import javax.crypto.{Cipher, KeyGenerator}
import javax.inject.Inject

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import client.Client.{GetOutcome, StartStreaming, UserIsOffline}
import com.cryptoutility.protocol.Events.{StreamingResult, UserInfo, Event}
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
  : (ActorRef, Cipher, Future[Future[StreamingResult]])

  def initializeClient: Flow[Event, Event, _]

  def validateClient(id: String): Future[_]
}

class DefaultClientService @Inject()(config: Configuration)
                     (implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext) extends ClientService{

  val registry = system.actorOf(Client.registry, "client-registry")
  implicit val timeout = Timeout(5 seconds)

  def startStreaming(clientId: String, filename: String, sender: String, contentType: Option[String] = None)
    : (ActorRef, Cipher, Future[Future[StreamingResult]]) = {
    val (user, ref) = Await.result(findClientById(clientId), timeout.duration)
    val (secret, cipher) = new EncryptionContext(user.key, config).cipher
    ref ! StartStreaming(filename, contentType.getOrElse(""), sender, secret)
    val streamingResult =  (ref ? GetOutcome).mapTo[Future[StreamingResult]]
    (ref, cipher, streamingResult)
  }

  def validateClient(id: String): Future[_] = ???

  def initializeClient: Flow[Event, Event, _] = Client.flow(registry, config)

  def findClientById(id: String): Future[(UserInfo, ActorRef)] = {
    (registry ? Client.Get(id)).map{
        case (u: UserInfo, r: ActorRef) => (u, r)
        case UserIsOffline => throw new RuntimeException(s"client with id $id is currently offline")
      }
  }

}
