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
package io.kanaka.monadic.dsl

import cats.data.{OptionT, Validated}
import cats.instances.future._
import io.kanaka.monadic.dsl.compat.cats._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.mvc.Results
import play.api.test.PlaySpecification
import play.test.Helpers

import scala.concurrent.Future

/**
  * @author Valentin Kasas
  */
class CatsStepOpsSpec extends PlaySpecification with Results {

  implicit val app = Helpers.fakeApplication()

  "ActionDSL.cats" should {

    "properly promote OptionT[Future, A] to Step[A]" in {
      val optiontFutureRight: OptionT[Future, Int] = OptionT.fromOption[Future](Option(42))
      await((optiontFutureRight ?| NotFound).run) mustEqual Right(42)

      val optiontFutureLeft: OptionT[Future, Unit] = OptionT.fromOption[Future](None)
      await((optiontFutureLeft ?| NotFound).run) mustEqual Left(NotFound)
    }

    "properly promote Validated[B,A] to Step[A]" in {
      val valid = Validated.Valid(42)
      await((valid ?| NotFound).run) mustEqual Right(42)

      val fail = Validated.Invalid("Error")
      await((fail ?| NotFound).run) mustEqual Left(NotFound)
    }


    "properly promote Future[Validated[B,A]] to Step[A]" in {
      val valid = Future.successful(Validated.Valid(42))
      await((valid ?| NotFound).run) mustEqual Right(42)

      val fail:Future[Validated[String, Int]] = Future.successful(Validated.Invalid("Error"))
      await((fail ?| NotFound).run) mustEqual Left(NotFound)
    }
  }
}
