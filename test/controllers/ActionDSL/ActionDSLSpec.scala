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
package controllers.ActionDSL

import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.{JsError, JsSuccess}
import play.api.mvc.Results
import play.api.test.{FakeApplication, PlaySpecification}

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scalaz.EitherT
import scalaz.syntax.either._
import scala.concurrent.ExecutionContext.Implicits.global
/**
 * @author Valentin Kasas
 */
class ActionDSLSpec extends PlaySpecification with MonadicActions with Results {

  implicit val app = FakeApplication()

  "ActionDSL" should {

    "properly promote Future[A] to Step[A]" in {
      val successfulFuture = Future.successful(42)
      await((successfulFuture ?| NotFound).run) mustEqual 42.right

      val failedFuture = Future.failed[Int](new NullPointerException)
      await((failedFuture ?| NotFound).run) mustEqual NotFound.left
    }

    "properly promote Future[Option[A]] to Step[A]" in {
      val someFuture = Future.successful(Some(42))
      await((someFuture ?| NotFound).run) mustEqual 42.right

      val noneFuture = Future.successful[Option[Int]](None)
      await((noneFuture ?| NotFound).run) mustEqual NotFound.left
    }


    "properly promote Future[Either[B, A]] to Step[A]" in {
      val rightFuture = Future.successful[Either[String, Int]](Right(42))
      await((rightFuture ?| NotFound).run) mustEqual 42.right

      val leftFuture = Future.successful[Either[String, Int]](Left("foo"))
      val eitherT = leftFuture ?| (s => BadRequest(s))
      await(eitherT.run).toEither must beLeft

      val result = eitherT.run.map(_.swap.getOrElse(NotFound))
      status(result) mustEqual 400

      contentAsString(result) must contain("foo")
    }
    
    "properly promote EitherT[Future, ?, A] to Step[A]" in {
      val rightEitherT = EitherT.eitherT(Future.successful(42.right[String]))
      await((rightEitherT ?| NotFound).run) mustEqual 42.right

      val leftEitherT = EitherT.eitherT(Future.successful("foot".left[Int]))
      val step = leftEitherT ?| (s => BadRequest(s))
      await(step.run).toEither must beLeft

      val result = step.run.map(_.swap.getOrElse(NotFound))
      status(result) mustEqual 400

      contentAsString(result) must contain("foo")
    }

    "properly promote Future[B \\/ A] to Step[A]" in {
      val rightFuture = Future.successful(42.right[String])
      await((rightFuture ?| NotFound).run) mustEqual 42.right

      val leftFuture = Future.successful("foo".left[Int])
      val eitherT = leftFuture ?| (s => BadRequest(s))
      await(eitherT.run).toEither must beLeft

      val result = eitherT.run.map(_.swap.getOrElse(NotFound))
      status(result) mustEqual 400

      contentAsString(result) must contain("foo")
    }
    
    "properly promote Option[A] to Step[A]" in {
      val some = Some(42)
      await((some ?| NotFound).run) mustEqual 42.right
      
      val none = None
      await((none ?| NotFound).run) mustEqual NotFound.left
    }

    "properly promote Either[B, A] to Step[A]" in {
      val right = Right[String, Int](42)
      await((right ?| NotFound).run) mustEqual 42.right

      val left = Left[String, Int]("foo")
      val eitherT = left ?| (s => BadRequest(s))
      await(eitherT.run).toEither must beLeft

      val result = eitherT.run.map(_.swap.getOrElse(NotFound))
      status(result) mustEqual 400

      contentAsString(result) must contain ("foo")
    }

    "properly promote B \\/ A to Step[A]" in {
      val right = 42.right[String]
      await((right ?| NotFound).run) mustEqual 42.right

      val left = "foo".left[Int]
      val eitherT = left ?| (s => BadRequest(s))
      await(eitherT.run).toEither must beLeft

      val result = eitherT.run.map(_.swap.getOrElse(NotFound))
      status(result) mustEqual 400

      contentAsString(result) must contain("foo")
    }

    "properly promote JsResult[A] to Step[A]" in {
      val jsSuccess = JsSuccess(42)
      await((jsSuccess ?| NotFound).run) mustEqual 42.right

      val jsError = JsError("foo")
      val eitherT = jsError ?| (e => BadRequest(JsError.toJson(e)))
      await(eitherT.run).toEither must beLeft

      val result = eitherT.run.map(_.swap.getOrElse(NotFound))
      status(result) mustEqual 400

      contentAsString(result) must contain(JsError.toJson(jsError).toString())
    }

    "properly promote Form[A] to Step[A]" in {
      val successfulForm = Form(single("int" -> number), Map("int" -> "42"), Nil, Some(42))
      await((successfulForm ?| NotFound).run) mustEqual 42.right

      val erroneousForm = successfulForm.withError("int", "foo")
      import play.api.Play.current
      import play.api.i18n.Messages.Implicits._
      val eitherT = erroneousForm ?| (f => BadRequest(f.errorsAsJson))
      await(eitherT.run).toEither must beLeft

      val result = eitherT.run.map(_.swap.getOrElse(NotFound))
      status(result) mustEqual 400

      contentAsString(result) must contain(erroneousForm.errorsAsJson.toString())
    }
    
    "properly promote Boolean to Step[A]" in {
      await((true ?| NotFound).run) mustEqual ().right
      await((false ?| NotFound).run) mustEqual NotFound.left
    }
    
    "properly promote Try[A] to Step[A]" in {
      val success = Success(42)
      await((success ?| NotFound).run) mustEqual 42.right
      
      val failure = Failure(new Exception("foo"))
      val eitherT = failure ?| (e => BadRequest(e.getMessage))
      await(eitherT.run).toEither must beLeft

      val result = eitherT.run.map(_.swap.getOrElse(NotFound))
      status(result) mustEqual 400

      contentAsString(result) must contain("foo")
      
    }
  }


}
