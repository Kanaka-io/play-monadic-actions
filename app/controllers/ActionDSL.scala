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
import play.api.mvc.{Results, Result}
import scalaz.syntax.either._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}
import scalaz.syntax.std.option._
import scalaz._

/**
 * @author Valentin Kasas
 */
package object ActionDSL {

  type Step[A] = EitherT[Future, Result, A]
  type JsErrorContent = Seq[(JsPath, Seq[ValidationError])]

  private [ActionDSL] def fromFuture[A](onFailure: Throwable => Result)(future: Future[A])(implicit ec: ExecutionContext): Step[A] =
    EitherT[Future, Result, A](
      future.map(_.right).recover{
        case NonFatal(t) => onFailure(t).left
      }
    )

  private [ActionDSL] def fromFOption[A](onNone: => Result)(fOption: Future[Option[A]])(implicit ec: ExecutionContext): Step[A] =
    EitherT[Future, Result, A](fOption.map(_ \/> onNone))

  private [ActionDSL] def fromFEither[A,B](onLeft: B => Result)(fEither: Future[Either[B,A]])(implicit ec: ExecutionContext): Step[A] =
    EitherT[Future, Result, A](fEither.map(_.fold(onLeft andThen \/.left,\/.right)))

  private [ActionDSL] def fromFDisjunction[A,B](onLeft: B => Result)(fDisjunction: Future[B \/ A])(implicit ec: ExecutionContext): Step[A] =
    EitherT[Future, Result, A](fDisjunction.map(_.leftMap(onLeft)))

  private [ActionDSL] def fromOption[A](onNone: => Result)(option: Option[A]): Step[A] =
    EitherT[Future, Result, A](Future.successful(option \/> onNone))

  private [ActionDSL] def fromEither[A,B](onLeft: B => Result)(either: Either[B,A])(implicit ec: ExecutionContext): Step[A] =
    EitherT[Future, Result, A](Future.successful(either.fold(onLeft andThen \/.left, \/.right)))

  private [ActionDSL] def fromDisjunction[A,B](onLeft: B => Result)(disjunction: B \/ A)(implicit ec: ExecutionContext): Step[A] =
    EitherT[Future, Result, A](Future.successful(disjunction.leftMap(onLeft)))

  private [ActionDSL] def fromJsResult[A](onJsError: JsErrorContent => Result)(jsResult: JsResult[A]): Step[A] =
    EitherT[Future, Result, A](Future.successful(jsResult.fold(onJsError andThen \/.left, \/.right)))

  private [ActionDSL] def fromForm[A](onError: Form[A] => Result)(form: Form[A]): Step[A] =
    EitherT[Future, Result, A](Future.successful(form.fold(onError andThen \/.left, \/.right)))

  private [ActionDSL] def fromBoolean(onFalse: => Result)(boolean: Boolean): Step[Unit] =
    EitherT[Future, Result, Unit](Future.successful(if (boolean) ().right else onFalse.left))

  private [ActionDSL] def fromTry[A](onFailure: Throwable => Result)(tryValue: Try[A]):Step[A] =
    EitherT[Future, Result, A](Future.successful(tryValue match {
      case Failure(t) => onFailure(t).left
      case Success(v) => v.right
    }))

  trait StepOps[A, B] {
    def orFailWith(failureHandler: B => Result):Step[A]
    def ?|(failureHandler: B => Result): Step[A] = orFailWith(failureHandler)
    def ?|(failureThunk: => Result): Step[A] = orFailWith(_ => failureThunk)
  }


  trait MonadicActions {

    import scala.language.implicitConversions

    val executionContext: ExecutionContext = play.api.libs.concurrent.Execution.defaultContext

    implicit val futureIsAFunctor = new Functor[Future] {
      override def map[A, B](fa: Future[A])(f: (A) => B) = fa.map(f)(executionContext)
    }

    implicit val futureIsAMonad = new Monad[Future] {
      override def point[A](a: => A) = Future(a)(executionContext)

      override def bind[A, B](fa: Future[A])(f: (A) => Future[B]) = fa.flatMap(f)(executionContext)
    }

    // This instance is needed to enable filtering/pattern-matching in for-comprehensions
    // It is acceptable for pattern-matching
    implicit val resultIsAMonoid = new Monoid[Result] {
      override def zero = Results.InternalServerError

      override def append(f1: Result, f2: => Result) = throw new IllegalStateException("should not happen")
    }

    implicit class FutureOps[A](future: Future[A]) {
      def -| : Step[A] = EitherT[Future, Result, A](future.map(_.right)(executionContext))
    }

    implicit def futureToStepOps[A](future: Future[A]): StepOps[A, Throwable] = new StepOps[A, Throwable] {
      override def orFailWith(failureHandler: (Throwable) => Result) = fromFuture(failureHandler)(future)(executionContext)
    }

    implicit def fOptionToStepOps[A](fOption: Future[Option[A]]):StepOps[A,Unit] = new StepOps[A, Unit]{
      override def orFailWith(failureHandler: Unit => Result) = fromFOption(failureHandler(()))(fOption)(executionContext)
    }

    implicit def fEitherToStepOps[A, B](fEither: Future[Either[B,A]]): StepOps[A,B] = new StepOps[A,B] {
      override def orFailWith(failureHandler: (B) => Result) = fromFEither(failureHandler)(fEither)(executionContext)
    }

    implicit def fDisjunctionToStepOps[A, B](fDisjunction: Future[B \/ A]): StepOps[A,B] = new StepOps[A, B] {
      override def orFailWith(failureHandler: (B) => Result) = fromFDisjunction(failureHandler)(fDisjunction)(executionContext)
    }

    implicit def optionToStepOps[A](option: Option[A]):StepOps[A, Unit] = new StepOps[A, Unit] {
      override def orFailWith(failureHandler: (Unit) => Result) = fromOption(failureHandler(()))(option)
    }

    implicit def eitherToStepOps[A, B](either: Either[B,A]): StepOps[A,B] = new StepOps[A,B] {
      override def orFailWith(failureHandler: (B) => Result) = fromEither(failureHandler)(either)(executionContext)
    }

    implicit def disjunctionToStepOps[A, B](disjunction: B \/ A): StepOps[A,B] = new StepOps[A, B] {
      override def orFailWith(failureHandler: (B) => Result) = fromDisjunction(failureHandler)(disjunction)(executionContext)
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

    implicit def stepToResult[R <: Result](step: Step[R]): Future[Result] = step.run.map(_.toEither.merge)(executionContext)

    implicit def stepToEither[A](step: Step[A]): Future[Either[Result, A]] = step.run.map(_.toEither)(executionContext)
  }
}
