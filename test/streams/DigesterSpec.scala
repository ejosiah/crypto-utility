package streams

import java.security.{NoSuchAlgorithmException, MessageDigest}
import java.util.Base64

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.util.ByteString
import org.scalatestplus.play.PlaySpec
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.language.postfixOps

/**
  * Created by jay on 20/09/2016.
  */
class DigesterSpec extends PlaySpec{

  implicit val system = ActorSystem("test-system")
  implicit val mat = ActorMaterializer()

  def digest(msg: String, algorithm: String) = {
    val source = Source.single(ByteString(msg))
    val digester = Flow.fromGraph(new Digester(algorithm))
    val sink = Sink.head[String]

    Await.result(source.via(digester).toMat(sink)(Keep.right).run(), 1 second)
  }

  "Digester" should {
    "calculate correct digest of byteString" in {
      val MD5 = "MD5"
      val msgToDigest = "I'm feeling really luck :)"
      val expected = {
        val digester = MessageDigest.getInstance(MD5)
        val e = Base64.getEncoder.encode(digester.digest(msgToDigest.getBytes))
        new String(e)
      }

      val result = digest(msgToDigest, MD5)
      result mustBe expected
    }

    "trying to digest using an invalid algorithm should fail" in {
      a [NoSuchAlgorithmException] should be thrownBy{
        digest("I'm feeling really luck :)", "UNKNOWN")
      }
    }
  }


}
