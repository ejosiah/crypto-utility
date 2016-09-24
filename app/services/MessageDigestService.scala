package services

import java.security.{MessageDigest, Security}
import java.util.Base64
import javax.inject.Singleton

import akka.NotUsed
import akka.stream.javadsl.Keep
import akka.stream.scaladsl.{Flow, Sink}
import akka.util.ByteString
import streams.{Crypto, Digester}

import scala.collection.JavaConverters._
import scala.concurrent.Future
/**
  * Created by Josiah on 9/17/2016.
  */
@Singleton
class MessageDigestService {

  def algorithms: Seq[String] = Security.getAlgorithms("MessageDigest").asScala.toSeq

  def sink(algorithm: String): Sink[ByteString, Future[String]] = {
    Flow
      .fromGraph(Crypto.digester(algorithm))
      .toMat(Sink.head[String])((l, r) => r)
  }
}