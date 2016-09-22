package client.protocol

import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.util.ByteString
import client.protocol.Events.{InvalidFormatException, EventSerializer, Initialized}
import org.scalatestplus.play.PlaySpec
import play.api.http.websocket.BinaryMessage

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

/**
  * Created by jay on 20/09/2016.
  */
class EventDecoderSpec extends PlaySpec{

  implicit val system = ActorSystem("test-system")
  implicit val mat = ActorMaterializer()

  "event decode" should{
    "decode byte streams to an event" in {
      val expected = Initialized(true, UserInfo("James", "Carl", "james@example.com", None))
      val serialized = EventSerializer.serialize(expected)

      val f =
        Source.single(BinaryMessage(ByteString(serialized)))
          .via(new EventDecoder)
            .toMat(Sink.head)(Keep.right).run()

      val actual = Await.result(f, 500 millis)

      actual mustBe expected
    }
    "of an invalid byte stream" in {
      a [InvalidFormatException] should be thrownBy{
        val data = new Array[Byte](256)
        Random.nextBytes(data)
        EventSerializer.deserialize(data)
      }
    }
  }

}
