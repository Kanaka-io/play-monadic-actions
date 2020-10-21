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

import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n._
import play.api.libs.json.{JsError, JsSuccess}
import play.api.mvc.{Result, Results}
import play.api.test.PlaySpecification
import play.test.Helpers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}
/**
 * @author Valentin Kasas
 */
class DSLSpec extends PlaySpecification with Results {

  implicit val app = Helpers.fakeApplication()

  implicit val messages = MessagesImpl(Lang.defaultLang, new DefaultMessagesApi())

  "dsl" should {

    "properly promote Future[A] to Step[A]" in {
      val successfulFuture = Future.successful(42)
      await((successfulFuture ?| NotFound).run) mustEqual Right(42)

      val failedFuture = Future.failed[Int](new NullPointerException)
      await((failedFuture ?| NotFound).run) mustEqual Left(NotFound)

      await((successfulFuture -| escalate).run) mustEqual Right(42)
      await((failedFuture -| escalate).run) must throwA[NullPointerException]
    }

    "properly promote Future[Option[A]] to Step[A]" in {
      val someFuture = Future.successful(Some(42))
      await((someFuture ?| NotFound).run) mustEqual Right(42)

      val noneFuture = Future.successful[Option[Int]](None)
      await((noneFuture ?| NotFound).run) mustEqual Left(NotFound)
    }


    "properly promote Future[Either[B, A]] to Step[A]" in {
      val rightFuture = Future.successful[Either[String, Int]](Right(42))
      await((rightFuture ?| NotFound).run) mustEqual Right(42)

      val leftFuture = Future.successful[Either[String, Int]](Left("foo"))
      val step = leftFuture ?| (s => BadRequest(s))
      await(step.run) must beLeft

      val result = step.run.map(_.fold(identity, _ => NotFound))
      status(result) mustEqual 400

      contentAsString(result) must contain("foo")
    }
    
    "properly promote Option[A] to Step[A]" in {
      val some = Some(42)
      await((some ?| NotFound).run) mustEqual Right(42)
      
      val none = None
      await((none ?| NotFound).run) mustEqual Left(NotFound)
    }

    "properly promote Either[B, A] to Step[A]" in {
      val right = Right[String, Int](42)
      await((right ?| NotFound).run) mustEqual Right(42)

      val left = Left[String, Int]("foo")
      val step = left ?| (s => BadRequest(s))
      await(step.run) must beLeft

      val result:Future[Result] = step.run.map(_.fold(identity, _ => NotFound))
      status(result) mustEqual 400

      contentAsString(result) must contain ("foo")
    }

    "properly promote JsResult[A] to Step[A]" in {
      val jsSuccess = JsSuccess(42)
      await((jsSuccess ?| NotFound).run) mustEqual Right(42)

      val jsError = JsError("foo")
      val step = jsError ?| (e => BadRequest(JsError.toJson(e)))
      await(step.run) must beLeft

      val result = step.run.map(_.fold(identity, _ => NotFound))
      status(result) mustEqual 400

      contentAsString(result) must contain(JsError.toJson(jsError).toString())
    }

    "properly promote Form[A] to Step[A]" in {
      val successfulForm = Form(single("int" -> number), Map("int" -> "42"), Nil, Some(42))
      await((successfulForm ?| NotFound).run) mustEqual Right(42)

      val erroneousForm = successfulForm.withError("int", "foo")
      import play.api.i18n.Messages.Implicits._
      val step = erroneousForm ?| (f => BadRequest(f.errorsAsJson))
      await(step.run) must beLeft

      val result = step.run.map(_.fold(identity, _ => NotFound))
      status(result) mustEqual 400

      contentAsString(result) must contain(erroneousForm.errorsAsJson.toString())
    }

    "properly promote Boolean to Step[A]" in {
      await((true ?| NotFound).run) mustEqual Right(())
      await((false ?| NotFound).run) mustEqual Left(NotFound)
    }

    "properly promote FutureBoolean[Boolean] to Step[Unit]" in {
      await((Future(false) ?| NotFound).run) mustEqual Left(NotFound)
      await((Future(true) ?| NotFound).run) mustEqual Right(())
    }

    "properly promote Try[A] to Step[A]" in {
      val success = Success(42)
      await((success ?| NotFound).run) mustEqual Right(42)
      
      val failure = Failure(new Exception("foo"))
      val step = failure ?| (e => BadRequest(e.getMessage))
      await(step.run) must beLeft

      val result: Future[Result] = step.run.map(_.fold(identity, _ => NotFound))
      status(result) mustEqual 400

      contentAsString(result) must contain("foo")
      
    }

    "support filtering in for-comprehensions" in {
      val someValueSatisfyingPredicate: Future[Option[Int]] = Future.successful(Some(20))

      val res1 = for {
        a <- someValueSatisfyingPredicate ?| NotFound if a < 42
      } yield Ok

      status(res1.run.map(_.merge)) mustEqual 200

      val noneValue: Future[Option[Int]] = Future.successful(None)


      val res2 = for {
        a <- noneValue ?| NotFound if a < 42
      } yield Ok

      status(res2.run.map(_.merge)) mustEqual 404


      val someValueFailingPredicate = Future.successful(Some(64))

      val res3 = for {
        a <- someValueFailingPredicate ?| NotFound if a < 42
      } yield Ok

      await(res3.run) must throwA[NoSuchElementException]


    }
  }


}
