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

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Results
import play.api.test.{FakeApplication, PlaySpecification}

import compat.scalaz._

import scala.concurrent.Future
import scalaz.syntax.either._
import scalaz.syntax.std.option._
import scalaz.syntax.validation._
import scalaz.std.scalaFuture._

/**
 * @author Valentin Kasas
 */
class ScalazStepOpsSpec extends PlaySpecification with Results {

  import scalaz._

  implicit val app = FakeApplication()

  "ActionDSL.scalaz" should {

    "properly promote B \\/ A to Step[A]" in {
      val aRight = 42.right[String]
      await((aRight ?| NotFound).run) mustEqual Right(42)

      val aLeft = "Error".left[Int]
      await((aLeft ?| NotFound).run) mustEqual Left(NotFound)
    }

    "properly promote Future[B \\/ A] to Step[A]" in {
      val futureRight = Future.successful(42.right[String])
      await((futureRight ?| NotFound).run) mustEqual Right(42)

      val futureLeft = Future.successful("Error".left[Int])
      await((futureLeft ?| NotFound).run) mustEqual Left(NotFound)
    }

    "properly promote EitherT[Future, B, A] to Step[A]" in {
      val eitherTRight = EitherT(Future.successful(42.right[String]))
      await((eitherTRight ?| NotFound).run) mustEqual Right(42)

      val eitherTLeft = EitherT(Future.successful("Error".left[Int]))
      await((eitherTLeft ?| NotFound).run) mustEqual Left(NotFound)
    }

    "properly promote OptionT[Future, A] to Step[A]" in {
      val someT = OptionT(Future.successful(42.some))
      await((someT ?| NotFound).run) mustEqual Right(42)

      val noneT = OptionT.none[Future,Int]
      await((noneT ?| NotFound).run) mustEqual Left(NotFound)
    }

    "properly promote Validation[B,A] to Step[A]" in {
      val valid = 42.success[String]
      await((valid ?| NotFound).run) mustEqual Right(42)

      val fail = "Error".failure[Int]
      await((fail ?| NotFound).run) mustEqual Left(NotFound)
    }


    "properly promote Future[Validation[B,A]] to Step[A]" in {
      val valid = Future.successful(42.success[String])
      await((valid ?| NotFound).run) mustEqual Right(42)

      val fail = Future.successful("Error".failure[Int])
      await((fail ?| NotFound).run) mustEqual Left(NotFound)
    }


  }

}
