package utili

/**
  * Created by jay on 21/09/2016.
  */
object Units {

  implicit class DigitalUnits(underLying: Int){

    def bytes: Int = underLying

    def kiloByte: Int = underLying * 1024
    def kiloBytes: Int = kiloByte
    def kb: Int = kiloBytes

    def megaByte: Int = kiloByte * 1024
    def megaBytes: Int = megaByte
    def mb = megaByte

  }
}
