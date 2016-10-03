package client

import java.security.{Key, PublicKey}
import javax.crypto.{KeyGenerator, Cipher}

import com.cryptoutility.protocol.crypto.{Base64Encode, Encrypt}
import play.api.Configuration

class EncryptionContext (publicKey: PublicKey, config: Configuration) {
  
  val symmetricAlgorithm = config.getString("cipher.symmetric.algorithm").get
  val asymmetricAlgorithm = config.getString("cipher.asymmetric.algorithm").get
  val secretKeyAlgorithm = config.getString("cipher.symmetric.key.algorithm").get

  def encrypt = Encrypt(symmetricAlgorithm, identity)(_, _)

  def cipher = {
    val key = secret
    val c = Cipher.getInstance(symmetricAlgorithm)
    c.init(Cipher.ENCRYPT_MODE, key)
    (key, c)
  }

  def secret = KeyGenerator.getInstance(secretKeyAlgorithm).generateKey()

  def encryptEncoded = Encrypt(symmetricAlgorithm, Base64Encode(_))(_, _)
  def wrap = Encrypt.wrap(asymmetricAlgorithm,  publicKey, Base64Encode(_))(_)
}
