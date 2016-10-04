package controllers

import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import com.cryptoutility.protocol.Events.StreamingResult
import play.api.libs.streams.Accumulator
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.{Action, Controller, RequestHeader}
import play.core.parsers.Multipart._
import services.EncryptionService

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

@Singleton
class EncryptionController @Inject()(encryptionService: EncryptionService)
                                    (implicit system: ActorSystem, ec: ExecutionContext) extends Controller{


  def index = Action.async {
    Future.successful( Ok(views.html.Encrypt.index() ))
  }

  def upload = Action.async(Utility.multipartFormData(digest)){ implicit  request =>
    val status: Future[StreamingResult] = request.body.files.head.ref
    status.map(s => Ok(views.html.Encrypt.status(s.wasSuccessful)))
  }

  def digest(rh: RequestHeader): FilePartHandler[Future[StreamingResult]] = {
    case FileInfo(key, fileName, contentType) =>
      val Some(clientId) = rh.session.get("userId")   // TODO fail if no clientId
      val Some(Seq(sender, _*)) = rh.queryString.get("sender")
      Accumulator(encryptionService.sink(clientId, fileName, contentType, sender)).map{ st =>
        FilePart(key, fileName, contentType, st)
      }
  }
}
