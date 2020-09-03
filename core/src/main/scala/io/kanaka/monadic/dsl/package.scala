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
package io.kanaka.monadic

import play.api.libs.json.JsPath
import play.api.mvc.Result
import play.api.data.Form
import play.api.libs.json.JsResult

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

import scala.language.implicitConversions

/**
  * @author Valentin Kasas
  */
package object dsl {

  case object escalate

  type JsErrorContent = collection.Seq[(JsPath, collection.Seq[play.api.libs.json.JsonValidationError])]

  implicit class FutureOps[A](future: Future[A])(implicit ec: ExecutionContext) {
    @deprecated("Use infix `-| escalate` instead", "2.0.1")
    def -| : Step[A] = Step(future.map(Right(_)))

    def -| (escalateWord: escalate.type): Step[A] = Step(future.map(Right(_)))
  }

  implicit def futureToStepOps[A](future: Future[A])(
      implicit ec: ExecutionContext): StepOps[A, Throwable] =
    new StepOps[A, Throwable] {
      override def orFailWith(failureHandler: (Throwable) => Result) =
        fromFuture(failureHandler)(future)
    }

  implicit def fOptionToStepOps[A](fOption: Future[Option[A]])(
      implicit ec: ExecutionContext): StepOps[A, Unit] =
    new StepOps[A, Unit] {
      override def orFailWith(failureHandler: Unit => Result) =
        fromFOption(failureHandler(()))(fOption)
    }

  implicit def fEitherToStepOps[A, B](fEither: Future[Either[B, A]])(
      implicit ec: ExecutionContext): StepOps[A, B] =
    new StepOps[A, B] {
      override def orFailWith(failureHandler: (B) => Result) =
        fromFEither(failureHandler)(fEither)
    }

  implicit def optionToStepOps[A](option: Option[A])(
      implicit ec: ExecutionContext): StepOps[A, Unit] =
    new StepOps[A, Unit] {
      override def orFailWith(failureHandler: (Unit) => Result) =
        fromOption(failureHandler(()))(option)
    }

  implicit def eitherToStepOps[A, B](either: Either[B, A])(
      implicit ec: ExecutionContext): StepOps[A, B] =
    new StepOps[A, B] {
      override def orFailWith(failureHandler: (B) => Result) =
        fromEither(failureHandler)(either)
    }

  implicit def jsResultToStepOps[A](jsResult: JsResult[A])(
      implicit ec: ExecutionContext): StepOps[A, JsErrorContent] =
    new StepOps[A, JsErrorContent] {
      override def orFailWith(failureHandler: (JsErrorContent) => Result) =
        fromJsResult(failureHandler)(jsResult)
    }

  implicit def formToStepOps[A](form: Form[A])(
      implicit ec: ExecutionContext): StepOps[A, Form[A]] =
    new StepOps[A, Form[A]] {
      override def orFailWith(failureHandler: (Form[A]) => Result) =
        fromForm(failureHandler)(form)
    }

  implicit def booleanToStepOps(boolean: Boolean)(
      implicit ec: ExecutionContext): StepOps[Unit, Unit] =
    new StepOps[Unit, Unit] {
      override def orFailWith(failureHandler: (Unit) => Result) =
        fromBoolean(failureHandler(()))(boolean)
    }

  implicit def tryToStepOps[A](tryValue: Try[A])(
      implicit ec: ExecutionContext): StepOps[A, Throwable] =
    new StepOps[A, Throwable] {
      override def orFailWith(failureHandler: (Throwable) => Result) =
        fromTry(failureHandler)(tryValue)
    }

  implicit def stepToResult[R <: Result](step: Step[R])(
      implicit ec: ExecutionContext): Future[Result] =
    step.run.map(_.merge)

  implicit def stepToEither[A](step: Step[A]): Future[Either[Result, A]] =
    step.run

  private[dsl] def fromFuture[A](onFailure: Throwable => Result)(
      future: Future[A])(implicit ec: ExecutionContext): Step[A] =
    Step(
        run = future.map(Right[Result, A](_)).recover {
          case t: Throwable => Left[Result, A](onFailure(t))
        }
    )

  private[dsl] def fromFOption[A](onNone: => Result)(
      fOption: Future[Option[A]])(implicit ec: ExecutionContext): Step[A] =
    Step(
        run = fOption.map {
          case Some(a) => Right(a)
          case None => Left(onNone)
        }
    )

  private[dsl] def fromFEither[A, B](onLeft: B => Result)(
      fEither: Future[Either[B, A]])(implicit ec: ExecutionContext): Step[A] =
    Step(
        run = fEither.map(_.left.map(onLeft))
    )

  private[dsl] def fromOption[A](onNone: => Result)(option: Option[A])(
      implicit ec: ExecutionContext): Step[A] =
    Step(
        run = Future.successful(
            option.fold[Either[Result, A]](Left(onNone))(Right(_)))
    )

  private[dsl] def fromEither[A, B](onLeft: B => Result)(either: Either[B, A])(
      implicit ec: ExecutionContext): Step[A] =
    Step(
        run = Future.successful(either.left.map(onLeft))
    )

  private[dsl] def fromJsResult[A](onJsError: JsErrorContent => Result)(
      jsResult: JsResult[A])(implicit ec: ExecutionContext): Step[A] =
    Step(
        run = Future.successful(
            jsResult.fold(err => Left(onJsError(err)), Right(_)))
    )

  private[dsl] def fromForm[A](onError: Form[A] => Result)(form: Form[A])(
      implicit ec: ExecutionContext): Step[A] =
    Step(
        run = Future.successful(form.fold(err => Left(onError(err)), Right(_)))
    )

  private[dsl] def fromBoolean(onFalse: => Result)(boolean: Boolean)(
      implicit ec: ExecutionContext): Step[Unit] =
    Step(
        run = Future.successful(if (boolean) Right(()) else Left(onFalse))
    )

  private[dsl] def fromTry[A](onFailure: Throwable => Result)(
      tryValue: Try[A])(implicit ec: ExecutionContext): Step[A] =
    Step(
        run = Future.successful(tryValue match {
          case Failure(t) => Left(onFailure(t))
          case Success(v) => Right(v)
        })
    )
}
