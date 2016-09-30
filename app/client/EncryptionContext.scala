package client

import java.security.PublicKey

import com.cryptoutility.protocol.crypto.{Base64Encode, Encrypt}
import play.api.Configuration

class EncryptionContext (publicKey: PublicKey, config: Configuration) {
  
  val symmetricAlgorithm = config.getString("cipher.symmetric.algorithm").get
  val asymmetricAlgorithm = config.getString("cipher.asymmetric.algorithm").get
  val secretKeyAlgorithm = config.getString("cipher.symmetric.key.algorithm").get

  def encrypt = Encrypt(symmetricAlgorithm, identity)(_, _)
  def encryptEncoded = Encrypt(symmetricAlgorithm, Base64Encode(_))(_, _)
  def wrap = Encrypt.wrap(asymmetricAlgorithm,  publicKey, Base64Encode(_))(_)
}
