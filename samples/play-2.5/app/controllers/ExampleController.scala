package controllers

import play.api.mvc.{Action, Controller}
import io.kanaka.monadic.dsl._, compat.cats._
import cats.data.Xor
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
/**
  * @author Valentin Kasas
  */
class ExampleController extends Controller {

  def index = Action.async {
    req =>
      for {
        i <- Future.successful(Xor.Right(42)) ?| NotFound
      } yield Ok(i.toString)
  }

}
