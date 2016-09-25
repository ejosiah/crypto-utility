package client

import java.util.UUID

import akka.actor.Actor.Receive
import akka.actor._
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{OverflowStrategy, Materializer}
import client.protocol.{EventEncoder, EventDecoder}
import com.cryptoutility.protocol.Events._
import play.api.libs.streams.ActorFlow
import play.api.Logger
import scala.collection.mutable
import scala.language.postfixOps

object Client{

  case class Online(user: UserInfo)
  case class OffLine(user: UserInfo)
  case class Get(clientId: String)

  def props(registry: ActorRef)(out: ActorRef) = Props(classOf[Client], out, registry)

  def registry = Props(classOf[ClientRegistry])

  def flow(registry: ActorRef)(implicit factory: ActorRefFactory, mat: Materializer): Flow[Event, Event, _] = {
    ActorFlow.actorRef(props(registry), overflowStrategy = OverflowStrategy.fail)
  }
}

class Client(out: ActorRef, registry: ActorRef) extends Actor{
  import Client._
  implicit val ec = context.system.dispatcher
  var mayBeUser = Option.empty[UserInfo]

  override def preStart = Logger.info(s"$self connected to server")

  override def receive = {
    case Initialized(isNew, user) =>
      mayBeUser = Some(user)
      registry ! Online(user)
      sender ! UserCreated(user)
  }


  override def postStop(): Unit = Logger.info(s"$self disconnected from server")
}

class ClientRegistry extends Actor{
  import Client._

  var users = mutable.Map[String,  (UserInfo, ActorRef)]()
  var refs = Set[ActorRef]()

  override def receive = {
    case Online(user) =>
      users += user.clientId -> (user -> sender())
      Logger.info(s"$user registered")
    case OffLine(user) => users -= user.clientId
    case Get(clientId) => sender() ! users.get(clientId)
    case (ref: ActorRef, msg) => ref forward msg
  }
}

