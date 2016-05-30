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

import cats.data.{Validated, Xor}
import compat.cats._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Results
import play.api.test.{FakeApplication, PlaySpecification}

import scala.concurrent.Future

/**
  * @author Valentin Kasas
  */
class CatsStepOpsSpec extends PlaySpecification with Results {

  implicit val app = FakeApplication()

  "ActionDSL.cats" should {

    "properly promote B Xor A to Step[A]" in {
      val aRight: String Xor Int = Xor.right(42)
      await((aRight ?| NotFound).run) mustEqual Right(42)

      val aLeft: String Xor Int = Xor.left("Error")
      await((aLeft ?| NotFound).run) mustEqual Left(NotFound)
    }

    "properly promote Future[B Xor A] to Step[A]" in {
      val futureRight = Future.successful(Xor.right(42))
      await((futureRight ?| NotFound).run) mustEqual Right(42)

      val futureLeft = Future.successful(Xor.left("Error"))
      await((futureLeft ?| NotFound).run) mustEqual Left(NotFound)
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
