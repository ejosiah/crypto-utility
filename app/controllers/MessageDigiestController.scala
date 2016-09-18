package controllers

import javax.inject._

import play.api.libs.streams.Accumulator
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.{Action, Controller, RequestHeader}
import play.core.parsers.Multipart.{FileInfo, FilePartHandler}
import services.MessageDigestService

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class MessageDigiestController @Inject()(digestService: MessageDigestService)(implicit exec: ExecutionContext) extends  Controller{


  def index = Action.async {
    Future.successful( Ok(views.html.MessageDigest.index(digestService.algorithms)) )
  }

  def upload = Action(Utility.multipartFormData(digest)){ implicit  request =>
    val digest = request.body.files.head.ref
    Ok(views.html.MessageDigest.digest(digest))
  }

  def digest(rh: RequestHeader): FilePartHandler[String] = {
    case FileInfo(key, fileName, contentType) =>
      val algo = rh.flash.get("algorithm").get
      Accumulator(digestService.sink(algo)).map{ d =>
       FilePart(key, fileName, contentType, digestService.finalize(d))
     }
  }
}