package client

import java.security.{Key, KeyPairGenerator}
import java.util.UUID
import javax.crypto.KeyGenerator

import akka.Done
import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{DefaultTimeout, ImplicitSender, TestKit}
import akka.util.ByteString
import client.Client.StreamingResult
import com.cryptoutility.protocol.Events.{StreamPart, StreamEnded, StreamStarted}
import com.cryptoutility.protocol.crypto.{MD5, Base64Encode, Encrypt}
import com.typesafe.config.ConfigFactory
import org.scalatest.{MustMatchers, WordSpecLike, Matchers, BeforeAndAfterAll}
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Try

/**
  * Created by jay on 29/09/2016.
  */
class StreamProcessorSpec extends TestKit(ActorSystem("test-system", ConfigFactory.parseString(""))) with DefaultTimeout with ImplicitSender
  with WordSpecLike with MustMatchers with BeforeAndAfterAll{

  def keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair()
  def id = UUID.randomUUID().toString
  def encrypt: (Array[Byte], Key) => Array[Byte] = (bytes, key) => bytes
  def encryptEncoded: (Array[Byte], Key) => String = (bytes, key) => new String(bytes)
  def wrap: Key => String = (k) => new String(k.getEncoded)
  val secret = KeyGenerator.getInstance("AES").generateKey()
  def f = "test.txt"
  def ct = "application/text"
  def fr = "test@example.com"
  lazy val streamProcessor = system.actorOf(Client.streamProcessor(self, id, encrypt, encryptEncoded, wrap))

  val sequence = {
    Seq(
      Client.StartStreaming(f, ct, fr, secret),
      ByteString("First line of content to encrypt"),
      ByteString("second line of content to encrypt"),
      ByteString("third line of content to encrypt"),
      ByteString("fourth line of content to encrypt"),
      Client.EndOfStream
    ).zipWithIndex
  }

  val digeset = MD5(
    (1 to 4)
      .map(sequence(_)._1)
      .map(_.asInstanceOf[ByteString])
      .map(_.toArray)
      .flatMap(identity[Array[Byte]]).toArray
  )

  val expected = {
    val key = secret
    Seq(
      StreamStarted(encryptEncoded(f.getBytes, key), encryptEncoded(ct.getBytes, key), encryptEncoded(fr.getBytes, key), wrap(secret)),
      StreamPart(0, encrypt("First line of content to encrypt".getBytes, key)),
      StreamPart(1, encrypt("second line of content to encrypt".getBytes, key)),
      StreamPart(2, encrypt("third line of content to encrypt".getBytes, key)),
      StreamPart(3, encrypt("fourth line of content to encrypt".getBytes, key)),
      StreamEnded(4, digeset),
      StreamingResult(4, Try(Done))
    ).zipWithIndex
  }

  "stream processor" should {
    "successfully encrypt the stream it receives" in {
      within(500 millis){

        sequence.foreach{ e =>
          val (msg, i) = e
          streamProcessor ! msg
          val reply = receiveOne(100 millis)
          (i, reply) match {
            case (_, StreamStarted(f0, ct0, fr0, _)) =>
              f0 mustBe f
              ct0 mustBe ct
              fr0 mustBe fr
            case (_, StreamPart(_, d)) =>
              new String(d) mustBe new String(expected(i)._1.asInstanceOf[StreamPart].chunk)
            case _ => reply mustBe expected(i)._1
          }
        }
      }
    }
  }

}
