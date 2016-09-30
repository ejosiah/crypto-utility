package controllers

import javax.inject._
import play.api._
import play.api.mvc._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject() extends Controller {

  /**
   * Create an Action to render an HTML page with a welcome message.
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index(userId: Option[String]) = Action { implicit req =>
    userId.map{ id =>
      // TODO check to make user's client is connected before accepting id
//      if(req.session.get("userId").isEmpty){
        Ok(views.html.index("Crypto Utility")).withSession("userId" -> id)
//      }else{
//        Ok(views.html.index("Crypto Utility"))
//      }
    }.getOrElse{
      Logger.warn("No client id provided")
      Ok(views.html.index("Crypto Utility"))
    }
  }

}
