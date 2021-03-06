package client.protocol

import java.security.KeyPairGenerator
import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import com.cryptoutility.protocol.Events._
import com.cryptoutility.protocol.EventSerializer
import org.scalatestplus.play.PlaySpec
import play.api.http.websocket.BinaryMessage

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Created by jay on 20/09/2016.
  */
class EventEncoderSpec extends PlaySpec{

  implicit val system = ActorSystem("test-system")
  implicit val mat = ActorMaterializer()

  def publicKey = {
    KeyPairGenerator.getInstance("RSA").generateKeyPair().getPublic
  }

  "event encode" should{
    "encode Event to a byte stream" in {
      val expected = Initialized(isNew = false, UserInfo("James", "Carl", "james@example.com", publicKey, UUID.randomUUID().toString))

      val f =
        Source.single(expected)
          .via(new EventEncoder)
            .toMat(Sink.head)(Keep.right).run()

      val bytes = Await.result(f, 500 millis).asInstanceOf[BinaryMessage].data.toArray
      val actual = EventSerializer.deserialize(bytes)
      actual mustBe expected
    }

  }

}
