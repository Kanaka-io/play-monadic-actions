package io.kanaka.play

import cats.data.{Validated, Xor}
import controllers.ActionDSL.MonadicActions
import io.kanaka.play.compat.cats._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Results
import play.api.test.{FakeApplication, PlaySpecification}

import scala.concurrent.Future

/**
  * @author Valentin Kasas
  */
class CatsStepOpsSpec extends PlaySpecification with MonadicActions with Results {

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
