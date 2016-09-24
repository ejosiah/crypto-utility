package streams

import java.security.Key
import java.security.spec.AlgorithmParameterSpec
import javax.crypto.Cipher

import akka.stream.{Outlet, Inlet, Attributes, FlowShape}
import akka.stream.stage.{OutHandler, InHandler, GraphStageLogic, GraphStage}
import akka.util.ByteString


class Encryptor(cipher: Cipher)
  extends GraphStage[FlowShape[ByteString, ByteString]]{

  val in = Inlet[ByteString]("Encryptor.in")
  val out = Outlet[ByteString]("Encryptor.out")

  def shape: FlowShape[ByteString, ByteString] = FlowShape.of(in, out)

  def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
    setHandler(in, new InHandler {
      override def onPush(): Unit = {
        val chunk = grab(in).toArray
        val encrypted = cipher.update(chunk)
        emit(out, ByteString(encrypted))

      }

      override def onUpstreamFinish(): Unit = {
        val last = cipher.doFinal()
        emit(out, ByteString(last))
        completeStage()
      }
    })

    setHandler(out, new OutHandler {
      override def onPull(): Unit =  pull(in)
    })
  }
}
