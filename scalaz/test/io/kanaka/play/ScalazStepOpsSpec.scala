package io.kanaka.play

import controllers.ActionDSL._
import io.kanaka.play.compat.scalaz._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Results
import play.api.test.{FakeApplication, PlaySpecification}

import scala.concurrent.Future
import scalaz.syntax.either._
import scalaz.syntax.validation._

/**
 * @author Valentin Kasas
 */
class ScalazStepOpsSpec extends PlaySpecification with MonadicActions with Results {

  import scalaz._

  implicit val app = FakeApplication()

  "ActionDSL.scalaz" should {

    "properly promote B \\/ A to Step[A]" in {
      val aRight: String \/ Int = 42.right
      await((aRight ?| NotFound).run) mustEqual Right(42)

      val aLeft = "Error".left
      await((aLeft ?| NotFound).run) mustEqual Left(NotFound)
    }

    "properly promote Future[B \\/ A] to Step[A]" in {
      val futureRight = Future.successful(42.right)
      await((futureRight ?| NotFound).run) mustEqual Right(42)

      val futureLeft = Future.successful("Error".left)
      await((futureLeft ?| NotFound).run) mustEqual Left(NotFound)
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
