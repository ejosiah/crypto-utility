package services

import java.security.Key
import javax.crypto.KeyGenerator

import akka.Done
import akka.actor.{Actor, ActorRef, Props}
import akka.stream.scaladsl.{Keep, Source}
import akka.util.{Timeout, ByteString}
import client.Client.{EndOfStream, GetOutcome, StreamingResult}
import client.ClientService
import org.scalamock.scalatest.MockFactory
import org.scalatestplus.play.PlaySpec
import streams.Actors

import scala.collection.immutable
import scala.concurrent.duration._
import scala.concurrent.{Await, Future, Promise}
import scala.language.postfixOps
import scala.util.{Failure, Random, Try}
import akka.pattern.ask

class EncryptionServiceSpec extends PlaySpec with Actors with MockFactory {
  implicit val timeout = Timeout(10 seconds)
  val clientService = stub[ClientService]
  val secret = KeyGenerator.getInstance("AES").generateKey()
  val nextSecret = new NextSecret {
    override def apply(): Key = secret
  }

  val actor = (failAt: Int) => system.actorOf(Props(new Actor{
    var processed = 0
    val promise = Promise[StreamingResult]()
    override def receive = {
      case b: ByteString =>
        processed = processed + 1
        if(processed == failAt)
          promise.success(StreamingResult(processed, Failure[Done](new Exception("Boom!"))))
      case EndOfStream   =>
        if(!promise.isCompleted)
          promise.success(StreamingResult(processed, Try(Done)))
      case GetOutcome    => sender ! promise.future
      case other => // Don't care
    }
  }))

  def nextBytes = {
    val b = new Array[Byte](256)
    Random.nextBytes(b)
    ByteString(b)
  }

  "Sink provided by EncryptionService" should {
    "successfully process all bytes streams it receives" in {

      val stream = immutable.Iterable(Seq.fill[ByteString](10)(nextBytes): _*)
      val source = Source[ByteString](stream)
      val act = actor(0)
      val sr = (act ? GetOutcome).mapTo[Future[StreamingResult]]
      clientService.startStreaming _ when("", "", "", None) returns ((act, sr))
      val sink =  new EncryptionService(clientService).sink("", "", None, "")

      val f: Future[StreamingResult] = source.toMat(sink)(Keep.right).run().flatMap(identity)

      val result = Await.result(f, 5 seconds)
      result mustBe StreamingResult(10, Try(Done))
    }

    "return a failure is something goes wrong during streaming" in {
      val stream = immutable.Iterable(Seq.fill[ByteString](10)(nextBytes): _*)
      val source = Source[ByteString](stream)
      val act = actor(4)
      val sr = (act ? GetOutcome).mapTo[Future[StreamingResult]]
      clientService.startStreaming _ when("", "", "", None) returns ((act, sr))
      val sink =  new EncryptionService(clientService).sink("", "", None, "")

      val f: Future[StreamingResult] = source.toMat(sink)(Keep.right).run().flatMap(identity)

      val result = Await.result(f, 5 seconds)
      result.count mustBe 4
      result.wasSuccessful mustBe false
    }
  }
}
