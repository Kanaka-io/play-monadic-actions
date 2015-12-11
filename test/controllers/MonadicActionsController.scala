package controllers

import controllers.ActionDSL.MonadicActions
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{Action, Controller}
import play.api.Play.current
import play.api.i18n.Messages.Implicits._

// the fact that this file compiles proves that https://github.com/Kanaka-io/play-monadic-actions/issues/1 is solved
object MonadicActionsController extends Controller with MonadicActions {

  val twoFieldForm = Form(
    tuple("id" -> longNumber,
      "message" -> nonEmptyText)
  )

  def twoFields = Action.async { implicit request =>
    for {
      (id , "foo") <- twoFieldForm.bindFromRequest ?| (formWithErrors => BadRequest(formWithErrors.errorsAsJson)) if false
    } yield { Ok("") }
  }
}