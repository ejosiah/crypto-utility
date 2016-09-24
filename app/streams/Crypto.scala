package streams

import akka.util.ByteString


object Crypto {

  def toHex(data: ByteString): String = toHex(data.toArray)

  def toHex(data: Array[Byte]): String = data.map{ b => Integer.toHexString(0xFF & b)}.mkString

  def digester(algorithm: String) = new Digester(algorithm)
}
