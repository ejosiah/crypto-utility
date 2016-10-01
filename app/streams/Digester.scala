package streams

import java.security.MessageDigest
import java.util.Base64

import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.util.ByteString
import com.cryptoutility.protocol.crypto.Hex

/**
  * Created by jay on 20/09/2016.
  */
class Digester(algorithm: String) extends GraphStage[FlowShape[ByteString, String]] {

  val in = Inlet[ByteString]("Digester.in")
  val out = Outlet[String]("Digester.out")
  val digester = MessageDigest.getInstance(algorithm)

  def shape: FlowShape[ByteString, String] = FlowShape.of(in, out)

  def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    setHandler(out, new OutHandler {
      override def onPull(): Unit = pull(in)
    })

    setHandler(in, new InHandler{

      def onPush(): Unit = {
        val chunk = grab(in)
        digester.update(chunk.toArray)
        pull(in)
      }

      @throws[Exception](classOf[Exception])
      override def onUpstreamFinish(): Unit = {
        val digest = {
          val d = digester.digest()
          Hex(d)
        }

        emit(out, digest)
        completeStage()
      }
    })
  }
}
