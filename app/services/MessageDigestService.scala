package services

import java.security.{MessageDigest, Security}
import java.util.Base64
import javax.inject.Singleton

import akka.stream.scaladsl.Sink
import akka.util.ByteString

import scala.collection.JavaConverters._
import scala.concurrent.Future
/**
  * Created by Josiah on 9/17/2016.
  */
@Singleton
class MessageDigestService {

  def algorithms: Seq[String] = Security.getAlgorithms("MessageDigest").asScala.toSeq

  def sink[Mat](algorithm: String): Sink[ByteString, Future[MessageDigest]] = {

    Sink.fold[MessageDigest, ByteString](MessageDigest.getInstance(algorithm)){ (digester, bytes) =>
      digester.update(bytes.toArray)
      digester
    }
  }

  def finalize(digester: MessageDigest): String = {
    val digest = digester.digest()
    new String(Base64.getEncoder.encode(digest))
  }
}