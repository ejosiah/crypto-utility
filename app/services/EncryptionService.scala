package services

import java.security.Key
import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import akka.stream.javadsl.Keep
import akka.stream.scaladsl.{Flow, Sink}
import akka.util.{ByteString, Timeout}
import client.Client.{EndOfStream, StreamingResult}
import client.ClientService
import streams.Encryptor

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

@Singleton
class EncryptionService @Inject()(clientService: ClientService)
                                 (implicit system: ActorSystem, ec: ExecutionContext) {

  implicit val timeout = Timeout(5 seconds)

  def sink(clientId: String, filename: String, contentType: Option[String], sender: String)
    : Sink[ByteString, Future[Future[StreamingResult]]] = {

    val (client, cipher, streamingResult) = clientService.startStreaming(clientId, filename, sender, contentType)

    Flow
      .fromGraph(new Encryptor(cipher))
      .toMat(Sink
        .actorRef(client, EndOfStream)
        .mapMaterializedValue(_ => streamingResult)
      )((l, r) => r)

  }

}
