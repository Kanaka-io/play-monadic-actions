package controllers

import play.api.mvc.{Action, Controller}
import io.kanaka.monadic.dsl._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
/**
  * @author Valentin Kasas
  */
object ExampleController extends Controller {

  def index = Action.async {
    req =>
      for {
        i <- Future.successful(Some(42)) ?| NotFound
      } yield Ok(i.toString)
  }

}
