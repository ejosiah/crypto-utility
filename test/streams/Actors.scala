package streams

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatestplus.play.PlaySpec

/**
  * Created by jay on 28/09/2016.
  */
trait Actors{ self: PlaySpec =>
  implicit val system = ActorSystem("test-system")
  implicit val mat = ActorMaterializer()
  implicit val ec = system.dispatcher
}
