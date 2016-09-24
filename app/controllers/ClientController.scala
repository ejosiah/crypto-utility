package controllers

import javax.inject._

import client.Client
import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.util.ByteString
import client.protocol.Codec
import com.cryptoutility.protocol.Events.Event
import play.api.mvc.{WebSocket, Controller}

@Singleton
class ClientController @Inject() (implicit system: ActorSystem, mat: Materializer) extends Controller {

  lazy val registry = system.actorOf(Client.registry, "client-registry")

  def handler = WebSocket.accept[Event, Event]{ r => Client.flow(registry) }(Codec())

}
