package client.protocol

import java.io._


import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.util.{Failure, Try, Success}
import utili.Units._
import scala.language.postfixOps

/**
  * packet format
  *   stream length               Event Id                  Event Body
  *  ----- ----- ----- -----    ----- ----- ----- -----    ----- ----- ----- -----
  *  |    |     |     |     | - |     |     |     |    | - |     |     |     |    ......
  *  ----- ----- ----- -----    ----- ----- ----- -----    ----- ----- ----- -----
  */
object Events {
  case class InvalidFormatException() extends Exception("Not an event stream")

  type Identity = Int
  type Header = Map[String, String]
  val EOF: Int = -1
  val IntSize = 4 bytes
  val HeaderSize = 8 bytes

  def toBytes(i: Int): Array[Byte] = {
    Array(
      ((i >>> 24) & 0xFF).asInstanceOf[Byte],
      ((i >>> 16) & 0xFF).asInstanceOf[Byte],
      ((i >>>  8) & 0xFF).asInstanceOf[Byte],
      ((i >>>  0) & 0xFF).asInstanceOf[Byte]
    )
  }

  def readInt(data: Array[Byte]): Int = {
    if(data.length < 4) throw new IllegalArgumentException(s"Not enough byte, 4 bytes required")
    (data(0) << 24) + ( data(1) << 16) + (data(2) << 8) + (data(3) << 0)
  }

  sealed trait Event{
    def id: Identity
  }

  trait EventListener{
    def on(event: Event): Future[Unit]
  }

  sealed trait EventSerializer[E <: Event]{
    def serialize(evt: E): Array[Byte]

    def deserialize(data: Array[Byte]): E

    def write(f: DataOutputStream => Unit) = {
      val byteStream = new ByteArrayOutputStream(1024)
      val out = new DataOutputStream(byteStream)
      f(out)
      byteStream.toByteArray
    }

    def read[T <: Event](data: Array[Byte])(f: DataInputStream => T) = {
      val in = new DataInputStream(new ByteArrayInputStream(data))
      Try (f(in)) match {
        case Failure(NonFatal(e)) =>
          val e1 = InvalidFormatException()
          e1.addSuppressed(e)
          throw e
        case Success(res) => res
      }
    }

  }

  object EventSerializer{

    def serializeUser(out: DataOutputStream, user: UserInfo) = {
      out.writeUTF(user.fname)
      out.writeUTF(user.lname)
      out.writeUTF(user.email)
      user.clientId.foreach(out.writeUTF)
    }

    def deserializeUser(in: DataInputStream, isNew: Boolean) = {
      val fname = in.readUTF()
      val lname = in.readUTF()
      val email = in.readUTF()
      val clientId = if (isNew) Option.empty[String] else Some(in.readUTF())
      UserInfo(fname, lname, email, clientId)
    }

    def serialize(event: Event): Array[Byte] = {
      val body = event match{
        case e: Initialized => InitializedSerializer.serialize(e)
        case e: UserCreated => UserCreatedSerializer.serialize(e)
      }
      val header = new ArrayBuffer[Byte](HeaderSize)
      header ++= toBytes(HeaderSize + body.length)
      header ++= toBytes(event.id)

      (header ++= body).toArray
    }

    def deserialize(data: Array[Byte]): Event = {
      val buf = ArrayBuffer(data:_*)
      val eventId = readInt(buf.slice(IntSize, HeaderSize).toArray)
      val body = buf.slice(HeaderSize, buf.size).toArray
      (eventId, body) match {
        case (0, b) => InitializedSerializer.deserialize(b)
        case(1, b) => UserCreatedSerializer.deserialize(b)
        case _ => throw InvalidFormatException()
      }
    }
  }
  import EventSerializer._

  case class Initialized(isNew: Boolean, user: UserInfo, id: Identity = 0) extends Event
  case class UserCreated(user: UserInfo, id: Identity = 1) extends Event

  object InitializedSerializer extends EventSerializer[Initialized]{

    override def serialize(evt: Initialized): Array[Byte] = write{ out =>
      out.writeBoolean(evt.isNew)
      serializeUser(out, evt.user)
    }

    def deserialize(data: Array[Byte]): Initialized = read(data){ in =>
      val isNew = in.readBoolean()
      val user = deserializeUser(in, isNew)
      Initialized(isNew, user)
    }
  }

  object UserCreatedSerializer extends EventSerializer[UserCreated]{

    def serialize(event: UserCreated): Array[Byte] = write( serializeUser(_, event.user))

    def deserialize(data: Array[Byte]): UserCreated = read(data)(in => UserCreated(deserializeUser(in, isNew = false)))
  }

}