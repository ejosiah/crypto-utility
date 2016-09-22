package client.protocol

import java.util.UUID

import client.protocol.Events.{EventSerializer, Initialized}
import org.scalatest.{MustMatchers, WordSpec, Spec}

/**
  * Created by jay on 21/09/2016.
  */
class InitializedEventSpec extends WordSpec with MustMatchers{

  "Initialised event serializer" should {
    "serialize and deserialize event" in {
      val expected = Initialized(false, UserInfo("James", "Carl", "james@example.com", Some(UUID.randomUUID().toString)))
      val serialized = EventSerializer.serialize(expected)
      val deserialized = EventSerializer.deserialize(serialized)

      deserialized mustBe expected
    }
  }

}
