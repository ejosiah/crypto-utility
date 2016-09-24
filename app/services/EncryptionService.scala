package services

import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import akka.stream.{Materializer, IOResult}
import akka.stream.scaladsl.{Flow, Sink}
import akka.util.ByteString
import streams.Crypto

import scala.concurrent.Future

@Singleton
class EncryptionService @Inject() (implicit system: ActorSystem, mat: Materializer) {

  def sink(filename: String, contentType: String): Sink[ByteString, Future[IOResult]] = ???

}
