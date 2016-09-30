package services

import java.security.Key
import javax.crypto.KeyGenerator

import akka.Done
import akka.actor.{Actor, ActorRef, Props}
import akka.stream.scaladsl.{Keep, Source}
import akka.util.ByteString
import client.Client.{EndOfStream, GetOutcome, StreamingResult}
import org.scalatestplus.play.PlaySpec
import streams.Actors

import scala.collection.immutable
import scala.concurrent.duration._
import scala.concurrent.{Await, Future, Promise}
import scala.language.postfixOps
import scala.util.{Failure, Random, Try}

class EncryptionServiceSpec extends PlaySpec with Actors{

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


  val findById = (actor: ActorRef) => new FindClientById() {
    def apply(id: String): Future[ActorRef] = Future.successful(actor)
  }

  def nextBytes = {
    val b = new Array[Byte](256)
    Random.nextBytes(b)
    ByteString(b)
  }

  "Sink provided by EncryptionService" should {
    "successfully process all bytes streams it receives" in {
      val stream = immutable.Iterable(Seq.fill[ByteString](10)(nextBytes): _*)
      val source = Source[ByteString](stream)
      val sink =  new EncryptionService(findById(actor(0)), nextSecret).sink("", "", "", "")

      val f: Future[StreamingResult] = source.toMat(sink)(Keep.right).run().flatMap(identity)

      val result = Await.result(f, 5 seconds)
      result mustBe StreamingResult(10, Try(Done))
    }

    "return a failure is something goes wrong during streaming" in {
      val stream = immutable.Iterable(Seq.fill[ByteString](10)(nextBytes): _*)
      val source = Source[ByteString](stream)
      val sink =  new EncryptionService(findById(actor(4)), nextSecret).sink("", "", "", "")

      val f: Future[StreamingResult] = source.toMat(sink)(Keep.right).run().flatMap(identity)

      val result = Await.result(f, 5 seconds)
      result.count mustBe 4
      result.wasSuccessful mustBe false
    }
  }
}
