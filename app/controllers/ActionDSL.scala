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

import play.api.data.Form
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsPath, JsResult}
import play.api.mvc.Result

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}
import scalaz.syntax.either._
import scalaz.syntax.std.option._
import scalaz.{EitherT, \/}

/**
 * @author Valentin Kasas
 */
object ActionDSL {

  type Step[A] = EitherT[Future, Result, A]
  type JsErrorContent = Seq[(JsPath, Seq[ValidationError])]

  def fromFuture[A](onFailure: Throwable => Result)(future: Future[A])(implicit ec: ExecutionContext): Step[A] =
    EitherT[Future, Result, A](
      future.map(_.right).recover{
        case NonFatal(t) => onFailure(t).left
      }
    )

  def fromFOption[A](onNone: => Result)(fOption: Future[Option[A]])(implicit ec: ExecutionContext): Step[A] =
    EitherT[Future, Result, A](fOption.map(_ \/> onNone))

  def fromFEither[A,B](onLeft: B => Result)(fEither: Future[Either[B,A]])(implicit ec: ExecutionContext): Step[A] =
    EitherT[Future, Result, A](fEither.map(_.fold(onLeft andThen \/.left,\/.right)))

  def fromFDisjunction[A,B](onLeft: B => Result)(fDisjunction: Future[B \/ A])(implicit ec: ExecutionContext): Step[A] =
    EitherT[Future, Result, A](fDisjunction.map(_.leftMap(onLeft)))

  def fromOption[A](onNone: => Result)(option: Option[A]): Step[A] =
    EitherT[Future, Result, A](Future.successful(option \/> onNone))

  def fromJsResult[A](onJsError: JsErrorContent => Result)(jsResult: JsResult[A]): Step[A] =
    EitherT[Future, Result, A](Future.successful(jsResult.fold(onJsError andThen \/.left, \/.right)))

  def fromForm[A](onError: Form[A] => Result)(form: Form[A]): Step[A] =
    EitherT[Future, Result, A](Future.successful(form.fold(onError andThen \/.left, \/.right)))

  def fromBoolean(onFalse: => Result)(boolean: Boolean): Step[Unit] =
    EitherT[Future, Result, Unit](Future.successful(if (boolean) ().right else onFalse.left))

  def fromTry[A](onFailure: Throwable => Result)(tryValue: Try[A]):Step[A] =
    EitherT[Future, Result, A](Future.successful(tryValue match {
      case Failure(t) => onFailure(t).left
      case Success(v) => v.right
    }))

  trait StepOps[A, B] {
    def orFailWith(failureHandler: B => Result):Step[A]
    def ?|(failureHandler: B => Result): Step[A] = orFailWith(failureHandler)
    def ?|(failureThunk: => Result): Step[A] = orFailWith(_ => failureThunk)
  }

  object Implicits {

    import scala.language.implicitConversions

    implicit def futureToStepOps[A](future: Future[A])(implicit ec: ExecutionContext): StepOps[A, Throwable] = new StepOps[A, Throwable] {
      override def orFailWith(failureHandler: (Throwable) => Result) = fromFuture(failureHandler)(future)
    }

    implicit def fOptionToStepOps[A](fOption: Future[Option[A]])(implicit ec: ExecutionContext):StepOps[A,Unit] = new StepOps[A, Unit]{
      override def orFailWith(failureHandler: Unit => Result) = fromFOption(failureHandler(()))(fOption)
    }

    implicit def fEitherToStepOps[A, B](fEither: Future[Either[B,A]])(implicit ec: ExecutionContext): StepOps[A,B] = new StepOps[A,B] {
      override def orFailWith(failureHandler: (B) => Result) = fromFEither(failureHandler)(fEither)
    }

    implicit def fDisjunctionToStepOps[A, B](fDisjunction: Future[B \/ A])(implicit ec: ExecutionContext): StepOps[A,B] = new StepOps[A, B] {
      override def orFailWith(failureHandler: (B) => Result) = fromFDisjunction(failureHandler)(fDisjunction)
    }

    implicit def optionToStepOps[A](option: Option[A]):StepOps[A, Unit] = new StepOps[A, Unit] {
      override def orFailWith(failureHandler: (Unit) => Result) = fromOption(failureHandler(()))(option)
    }

    implicit def jsResultToStepOps[A](jsResult: JsResult[A]): StepOps[A, JsErrorContent] = new StepOps[A, JsErrorContent] {
      override def orFailWith(failureHandler: (JsErrorContent) => Result) = fromJsResult(failureHandler)(jsResult)
    }

    implicit def formToStepOps[A](form: Form[A]): StepOps[A, Form[A]] = new StepOps[A, Form[A]] {
      override def orFailWith(failureHandler: (Form[A]) => Result) = fromForm(failureHandler)(form)
    }

    implicit def booleanToStepOps(boolean: Boolean): StepOps[Unit, Unit] = new StepOps[Unit, Unit] {
      override def orFailWith(failureHandler: (Unit) => Result) = fromBoolean(failureHandler(()))(boolean)
    }

    implicit def tryToStepOps[A](tryValue: Try[A]): StepOps[A, Throwable] = new StepOps[A, Throwable] {
      override def orFailWith(failureHandler: (Throwable) => Result) = fromTry(failureHandler)(tryValue)
    }

    implicit def stepToResult(step: Step[Result])(implicit ec: ExecutionContext): Future[Result] = step.run.map(_.merge)
  }
}