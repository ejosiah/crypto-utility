package client

import java.util.UUID

import akka.actor.Actor.Receive
import akka.actor._
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{OverflowStrategy, Materializer}
import client.protocol.Events.{UserCreated, Initialized, Event}
import client.protocol.{Events, EventEncoder, EventDecoder}
import play.api.libs.streams.ActorFlow
import play.api.Logger
import scala.language.postfixOps

object Client{

  def props(out: ActorRef) = Props(classOf[Client], out)

  def proxy(client: ActorRef) = Props(classOf[ClientProxy], client)

  def flow(implicit factory: ActorRefFactory, mat: Materializer): Flow[Event, Event, _] = {
    ActorFlow.actorRef(props, overflowStrategy = OverflowStrategy.fail)
  }

  def stringFlow(implicit factory: ActorRefFactory, mat: Materializer): Flow[String, String, _] = {
    ActorFlow.actorRef(props, overflowStrategy = OverflowStrategy.fail)
  }
}

class Client(out: ActorRef) extends Actor{

  implicit val ec = context.system.dispatcher

  override def preStart = Logger.info(s"$self connected to server")


  override def receive = {
    case Initialized(isNew, userInfo, _) =>
      val user = if(isNew){
        val clientId = UUID.randomUUID().toString
        userInfo.copy(clientId = Some(clientId))
      }else{
        userInfo
      }
      context.system.actorOf(Client.proxy(self), s"client-${user.clientId}")
      out ! UserCreated(user)
  }


  override def postStop(): Unit = Logger.info(s"$self disconnected from server")
}

class ClientProxy(client: ActorRef) extends Actor{
  override def receive = {
    case msg => client ! msg
  }
}

