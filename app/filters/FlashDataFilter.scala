package filters

import javax.inject.{Inject, Singleton}

import akka.stream.Materializer
import play.api.mvc.{Filter, RequestHeader, Result, Results}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by Josiah on 9/17/2016.
  */
@Singleton
class FlashDataFilter @Inject()(implicit override val mat: Materializer, exec: ExecutionContext) extends Filter  {

  def apply(nextFilter: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {
    rh.path match {
      case "/flash" =>
        // TODO add flash data to session
        Future.successful(Results.Ok)
      case _ =>
        nextFilter(rh).map { result =>
          // TODO remove flash data
          result
        }
    }
  }
}
