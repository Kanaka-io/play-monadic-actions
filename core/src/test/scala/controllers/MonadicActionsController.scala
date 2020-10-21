/*
 * Copyright 2014
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package controllers

import javax.inject.Inject

import io.kanaka.monadic.dsl._
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.InjectedController
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.i18n.Messages

// the fact that this file compiles proves that https://github.com/Kanaka-io/play-monadic-actions/issues/1 is solved
@Inject
class MonadicActionsController(messages: Messages) extends InjectedController {

  val twoFieldForm = Form(
    tuple("id" -> longNumber,
      "message" -> nonEmptyText)
  )

  def twoFields = Action.async { implicit request =>
    for {
      (id , "foo") <- twoFieldForm.bindFromRequest() ?| (formWithErrors => BadRequest(formWithErrors.errorsAsJson(messages))) if false
    } yield { Ok("") }
  }
}
