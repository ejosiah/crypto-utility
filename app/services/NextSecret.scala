package services

import java.security.Key
import javax.crypto.KeyGenerator
import javax.inject.Inject

import com.google.inject.Singleton
import play.api.Configuration

/**
  * Created by jay on 29/09/2016.
  */
trait NextSecret extends (() => Key)

class DefaultNextSecret @Inject() (config: Configuration) extends NextSecret{

  lazy val algorithm = config.getString("cipher.symmetric.key.algorithm").get

  override def apply(): Key = KeyGenerator.getInstance(algorithm).generateKey()
}