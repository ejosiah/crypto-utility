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

  def props(out: ActorRef) = Props(classOf[Client], out)

  def proxy(client: ActorRef) = Props(classOf[ClientRegistry], client)

  def flow(implicit factory: ActorRefFactory, mat: Materializer): Flow[Event, Event, _] = {
    ActorFlow.actorRef(props, overflowStrategy = OverflowStrategy.fail)
  }

  def stringFlow(implicit factory: ActorRefFactory, mat: Materializer): Flow[String, String, _] = {
    ActorFlow.actorRef(props, overflowStrategy = OverflowStrategy.fail)
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
    case msg => out ! msg
  }


  override def postStop(): Unit = Logger.info(s"$self disconnected from server")
}

class ClientRegistry(client: ActorRef) extends Actor{
  import Client._

  var users = mutable.Map[String,  (UserInfo, ActorRef)]()

  override def receive = {
    case Online(user) => users += user.clientId.get -> (user -> sender())
    case OffLine(user) => users -= user.clientId.get
    case Get(clientId) => sender() ! users.get(clientId)
    case msg => client ! msg
  }
}

