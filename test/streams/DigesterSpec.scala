package streams

import java.security.{NoSuchAlgorithmException, MessageDigest}
import java.util.Base64

import akka.NotUsed
import akka.actor.{Props, ActorSystem}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.util.ByteString
import client.Client.EndOfStream
import com.cryptoutility.protocol.crypto.{MD5, Hex}
import org.scalatestplus.play.PlaySpec
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.language.postfixOps
import scala.util.Random

/**
  * Created by jay on 20/09/2016.
  */
class DigesterSpec extends PlaySpec with Actors{

  def digest(msg: String, algorithm: String) = {
    val source = Source.single(ByteString(msg))
    val digester = Flow.fromGraph(new Digester(algorithm))
    val sink = Sink.head[String]

    Await.result(source.via(digester).toMat(sink)(Keep.right).run(), 1 second)
  }

  "Digester" should {
    "calculate correct digest of byteString" in {
      val msgToDigest = "I'm feeling really luck :)"
      val expected = MD5(msgToDigest)

      val result = digest(msgToDigest, MD5.algorithm)
      result mustBe expected
    }

    "trying to digest using an invalid algorithm" in {
      a [NoSuchAlgorithmException] should be thrownBy{
        digest("I'm feeling really luck :)", "UNKNOWN")
      }
    }
  }

  "Actor Digester" should{
    "calculate correct digest of byteString" in {
      val stream: Array[ByteString]  = Array.fill(10)(nextBytes)
      val expected = MD5(stream.map(_.toArray).flatMap(identity[Array[Byte]]))
      val (actor, f) = Crypto.actorDigester(MD5.algorithm)
      stream.foreach(actor ! _)
      actor ! EndOfStream
      val actual = Await.result(f, 5 seconds)
      actual mustBe expected
    }
    "trying to digest using an invalid algorithm" in {
      a [NoSuchAlgorithmException] should be thrownBy{
        Crypto.actorDigester("UNKNOWN")
      }
    }
  }

  def nextBytes = {
    val b = new Array[Byte](10)
    Random.nextBytes(b)
    ByteString(b)
  }


}
