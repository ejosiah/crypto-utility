package streams

import akka.util.ByteString
import com.cryptoutility.protocol.crypto.Hex


object Crypto {

  def toHex(bytes: ByteString) = Hex(bytes.toArray)

  def digester(algorithm: String) = new Digester(algorithm)
}
