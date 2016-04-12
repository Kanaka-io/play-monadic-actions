package controllers

import controllers.ActionDSL.MonadicActions
import play.api.mvc.{Action, Controller}

import scala.concurrent.Future
import scala.util.Try

/**
 * @author Valentin Kasas
 */
object ExampleController extends Controller with MonadicActions {

  def normalize(idStr: String) = Future.successful(idStr.trim)

  def service(id: Long) = Future.successful(Some(id * 2))

  def action(idStr: String) = Action.async {
    request =>
      for {
        normed <- normalize(idStr)   .-|
        id     <- Try(normed.toLong)  ?| BadRequest
        number <- service(id)         ?| NotFound
      } yield Ok(number.toString)
  }

}
