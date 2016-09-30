package streams


object Crypto {

  def digester(algorithm: String) = new Digester(algorithm)
}
