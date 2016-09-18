package filters

import javax.inject.{Inject, Singleton}

import akka.stream.Materializer
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by Josiah on 9/17/2016.
  */
@Singleton
class FlashDataFilter @Inject()(implicit override val mat: Materializer, exec: ExecutionContext) extends Filter  {

  def apply(nextFilter: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {
    rh.path match {
      case "/flash" =>
        rh.queryString.foreach(entry =>  rh.flash + (entry._1, entry._2.mkString(",")))
        Future.successful(Results.Ok)
      case _ => nextFilter(rh)
    }
  }
}
