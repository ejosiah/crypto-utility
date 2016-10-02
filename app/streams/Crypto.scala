package streams

import akka.actor.{ActorSystem, ActorRef, Props}
import akka.stream.ActorMaterializer
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.{Sink, Flow, Keep, Source}
import akka.util.ByteString
import client.Client.EndOfStream
import com.cryptoutility.protocol.crypto.Hex
import play.libs.concurrent.Futures

import scala.annotation.tailrec
import scala.concurrent.Future


object Crypto {

  def toHex(bytes: ByteString) = Hex(bytes.toArray)

  def digester(algorithm: String) = new Digester(algorithm)

  def actorDigester(algorithm: String)(implicit system: ActorSystem, mat: ActorMaterializer)
    : (ActorRef, Future[String]) = Source.actorPublisher[ByteString](Props(new ActorPublisher[ByteString]{
    import akka.stream.actor.ActorPublisherMessage._

    var pending = Vector.empty[ByteString]
    var end = false
    def receive = {
      case b: ByteString =>
        if(pending.isEmpty && totalDemand > 0 && !end){
          onNext(b)
        }else{
          pending :+= b
          processPending()
        }
      case EndOfStream =>
        end = true
        if(pending.nonEmpty){
          processPending()
        }else {
          onComplete()
        }
      case Request(_) =>
        processPending()
      case Cancel => context.stop(self)
    }

    @tailrec
    def processPending(): Unit = {
      if(end && pending.isEmpty)
        onComplete()
      else if (totalDemand > 0) {

        if (totalDemand <= Int.MaxValue) {
          val (use, keep) = pending.splitAt(totalDemand.toInt)
          pending = keep
          use foreach onNext
        } else {
          val (use, keep) = pending.splitAt(Int.MaxValue)
          pending = keep
          use foreach onNext
          processPending()
        }
      }

    }
  })
  ).viaMat(Flow.fromGraph(new Digester(algorithm)))(Keep.left)
    .toMat(Sink.head)(Keep.both).run()

}
