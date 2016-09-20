package controllers

import play.api.Play
import play.api.mvc.{BodyParser, MultipartFormData, RequestHeader}
import play.core.parsers.Multipart

/**
  * Created by Josiah on 9/18/2016.
  */
object Utility {

  import play.api.mvc.BodyParsers.parse._


  def multipartFormData[A](filePartHandler: RequestHeader => Multipart.FilePartHandler[A], maxLength: Long = DefaultMaxDiskLength): BodyParser[MultipartFormData[A]] = {
    BodyParser("multipartFormData") { request =>
      val app = play.api.Play.current // throw exception
    implicit val mat = app.materializer
      val bodyAccumulator = Multipart.multipartParser(DefaultMaxTextLength, filePartHandler(request)).apply(request)
     // TODO enforceMaxLength(request, maxLength, bodyAccumulator) or maybe not
      bodyAccumulator
    }
  }
}
