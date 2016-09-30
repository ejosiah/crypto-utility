package streams

import java.security.{Key, KeyFactory, MessageDigest, NoSuchAlgorithmException}
import java.util.Base64
import javax.crypto.{Cipher, KeyGenerator}

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.util.ByteString
import com.cryptoutility.protocol.crypto.Decrypt
import org.scalatestplus.play.PlaySpec

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import Cipher._

/**
  * Created by jay on 20/09/2016.
  */
class EncryptorSpec extends PlaySpec with Actors{

  val algorithm = "AES/ECB/PKCS5Padding"

  def decrypt = Decrypt.decrypt0(new String(_), algorithm)(_, _)

  def encryptionKey = {
    KeyGenerator.getInstance("AES").generateKey()
  }

  def encrypt(msg: String, cipher: Cipher) = {

    val res =
      Source.single(ByteString(msg))
        .via(Flow.fromGraph(new Encryptor(cipher)))
        .toMat(Sink.fold(ByteString())(_ ++ _))(Keep.right).run()

    Await.result(res, 5 second)
  }

  "Encryptor" should {
    "should encrypt supplied content" in {
      val clearText = "There is no way in the world anyone should know this"
      val key = encryptionKey


      val cipher = Cipher.getInstance(algorithm)
      cipher.init(Cipher.ENCRYPT_MODE, key)
      val actual = encrypt(clearText, cipher)

      val decrypted = decrypt(actual.toArray, key)

      decrypted mustBe clearText
    }
  }


}
