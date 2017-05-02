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

import play.api.mvc.{Action, Controller}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import scala.util.Try

import io.kanaka.monadic.dsl._
/**
  * @author Valentin Kasas
  */
object ExampleController extends Controller {


  def normalize(idStr: String) = Future.successful(idStr.trim)

  def service(id: Long) = Future.successful(Some(id * 2))

  def action(idStr: String) = Action.async {
    request =>
      for {
        normed <- normalize(idStr)    -| escalate
        id     <- Try(normed.toLong)  ?| BadRequest
        number <- service(id)         ?| NotFound
      } yield Ok(number.toString)
  }

}
