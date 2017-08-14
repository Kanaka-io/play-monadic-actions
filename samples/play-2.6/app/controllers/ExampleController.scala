package controllers

import javax.inject._
import play.api.mvc.{Action, AbstractController, ControllerComponents}
import io.kanaka.monadic.dsl._, compat.cats._
import scala.concurrent.ExecutionContext

import scala.concurrent.Future
/**
  * @author Valentin Kasas
  */
class ExampleController @Inject() (controllerComponents: ControllerComponents)(implicit ec: ExecutionContext) extends AbstractController(controllerComponents) {

  def index = Action.async {
    req =>
      for {
        i <- Future.successful(Right(42)) ?| NotFound
      } yield Ok(i.toString)
  }

}
