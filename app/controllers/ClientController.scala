package controllers

import javax.inject._

import client.ClientService
import client.protocol.Codec
import com.cryptoutility.protocol.Events.Event
import play.api.mvc.{Controller, WebSocket}

@Singleton
class ClientController @Inject()(clientService: ClientService) extends Controller {

  def handler = WebSocket.accept[Event, Event]{ r => clientService.initializeClient }(Codec())

}
