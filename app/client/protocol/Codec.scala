package client.protocol

import akka.stream.scaladsl.Flow
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.util.ByteString
import com.cryptoutility.protocol.EventSerializer
import com.cryptoutility.protocol.Events.Event
import play.api.http.websocket._
import play.api.mvc.WebSocket.MessageFlowTransformer

object Codec {

  def apply() = new MessageFlowTransformer[Event, Event] {
    def transform(flow: Flow[Event, Event, _]): Flow[Message, Message, _] = {
      Flow[Message]
        .via(new EventDecoder)
        .via(flow)
        .via(new EventEncoder)}
  }

}

import scala.collection.mutable.ArrayBuffer

class EventDecoder extends GraphStage[FlowShape[Message, Event]]{
  val in = Inlet[Message]("EventDecoder.in")
  val out = Outlet[Event]("EventDecoder.out")

  override def shape: FlowShape[Message, Event] = FlowShape.of(in, out)

  @throws[Exception](classOf[Exception])
  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
    val HeaderLength = 8  // 1st 4 bytes for length and next 4 bytes for event id
    var buf = ArrayBuffer[Byte]()
    var length = 0

    def reset(): Unit ={
      buf = ArrayBuffer[Byte]()
      length = 0
    }

    setHandler(in, new InHandler {
      import EventSerializer._

      override def onPush(): Unit = {
        grab(in) match {
          case BinaryMessage(data) => buf ++= data
          case CloseMessage(_, _) =>  completeStage()
          case msg => throw new IllegalArgumentException(s"Invalid message: $msg")
        }

        if(buf.length >= HeaderLength && length == 0) length = readInt(buf.slice(0, IntSize).toArray)

        if(buf.length == length){
          val decoded = deserialize(buf.toArray)
          reset()
          emit(out, decoded)
        }else{
          pull(in)
        }
      }

    })

    setHandler(out, new OutHandler {
      override def onPull(): Unit = pull(in)
    })
  }
}

class EventEncoder extends GraphStage[FlowShape[Event, Message]]{
  val in = Inlet[Event]("EventEncoder.in")
  val out = Outlet[Message]("EventEncoder.out")

  def shape: FlowShape[Event, Message] = FlowShape.of(in, out)

  def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    setHandler(in, new InHandler {
      def onPush(): Unit = {
        val event = grab(in)
        val msg = new BinaryMessage(ByteString(EventSerializer.serialize(event)))
        emit(out, msg)
      }
    })

    setHandler(out, new OutHandler {
      def onPull(): Unit = pull(in)
    })
  }

}