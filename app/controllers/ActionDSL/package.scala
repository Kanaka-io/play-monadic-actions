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

import io.kanaka.play.Step
import play.api.data.Form
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsPath, JsResult}
import play.api.mvc.{Result, Results}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
 * @author Valentin Kasas
 */
package object ActionDSL {

  type JsErrorContent = Seq[(JsPath, Seq[ValidationError])]

  private [ActionDSL] def fromFuture[A](onFailure: Throwable => Result)(future: Future[A])(implicit ec: ExecutionContext): Step[A] =
    Step(
      run = future.map(Right[Result, A](_)).recover{case t: Throwable => Left[Result, A](onFailure(t))},
      executionContext = ec
    )

  private [ActionDSL] def fromFOption[A](onNone: => Result)(fOption: Future[Option[A]])(implicit ec: ExecutionContext): Step[A] =
    Step(
      run = fOption.map{
        case Some(a) => Right(a)
        case None => Left(onNone)
      }(ec),
      executionContext = ec
    )

  private [ActionDSL] def fromFEither[A,B](onLeft: B => Result)(fEither: Future[Either[B,A]])(implicit ec: ExecutionContext): Step[A] =
    Step(
      run = fEither.map(_.left.map(onLeft)),
      executionContext = ec
    )

  private [ActionDSL] def fromOption[A](onNone: => Result)(option: Option[A])(implicit ec: ExecutionContext): Step[A] =
    Step(
      run = Future.successful(option.fold[Either[Result,A]](Left(onNone))(Right(_))),
      executionContext = ec
    )

  private [ActionDSL] def fromEither[A,B](onLeft: B => Result)(either: Either[B,A])(implicit ec: ExecutionContext): Step[A] =
    Step(
      run = Future.successful(either.left.map(onLeft)),
      executionContext = ec
    )

  private [ActionDSL] def fromJsResult[A](onJsError: JsErrorContent => Result)(jsResult: JsResult[A])(implicit ec: ExecutionContext): Step[A] =
    Step(
      run = Future.successful(jsResult.fold(err => Left(onJsError(err)), Right(_))),
      executionContext = ec
    )

  private [ActionDSL] def fromForm[A](onError: Form[A] => Result)(form: Form[A])(implicit ec: ExecutionContext): Step[A] =
    Step(
      run = Future.successful(form.fold(err => Left(onError(err)), Right(_))),
      executionContext = ec
    )

  private [ActionDSL] def fromBoolean(onFalse: => Result)(boolean: Boolean)(implicit ec:ExecutionContext): Step[Unit] =
    Step(
      run = Future.successful(if(boolean) Right(()) else Left(onFalse)),
      executionContext = ec
    )

  private [ActionDSL] def fromTry[A](onFailure: Throwable => Result)(tryValue: Try[A])(implicit ec: ExecutionContext):Step[A] =
    Step(
      run = Future.successful(tryValue match {
        case Failure(t) => Left(onFailure(t))
        case Success(v) => Right(v)
      }),
      executionContext = ec
    )


  trait StepOps[A, B] {
    def orFailWith(failureHandler: B => Result):Step[A]
    def ?|(failureHandler: B => Result): Step[A] = orFailWith(failureHandler)
    def ?|(failureThunk: => Result): Step[A] = orFailWith(_ => failureThunk)
  }


  trait MonadicActions {

    import scala.language.implicitConversions

    val executionContext: ExecutionContext = play.api.libs.concurrent.Execution.defaultContext

    implicit class FutureOps[A](future: Future[A]) {
      def -| : Step[A] = Step(future.map(Right(_))(executionContext), executionContext)
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

    implicit def optionToStepOps[A](option: Option[A]):StepOps[A, Unit] = new StepOps[A, Unit] {
      override def orFailWith(failureHandler: (Unit) => Result) = fromOption(failureHandler(()))(option)(executionContext)
    }

    implicit def eitherToStepOps[A, B](either: Either[B,A]): StepOps[A,B] = new StepOps[A,B] {
      override def orFailWith(failureHandler: (B) => Result) = fromEither(failureHandler)(either)(executionContext)
    }


    implicit def jsResultToStepOps[A](jsResult: JsResult[A]): StepOps[A, JsErrorContent] = new StepOps[A, JsErrorContent] {
      override def orFailWith(failureHandler: (JsErrorContent) => Result) = fromJsResult(failureHandler)(jsResult)(executionContext)
    }

    implicit def formToStepOps[A](form: Form[A]): StepOps[A, Form[A]] = new StepOps[A, Form[A]] {
      override def orFailWith(failureHandler: (Form[A]) => Result) = fromForm(failureHandler)(form)(executionContext)
    }

    implicit def booleanToStepOps(boolean: Boolean): StepOps[Unit, Unit] = new StepOps[Unit, Unit] {
      override def orFailWith(failureHandler: (Unit) => Result) = fromBoolean(failureHandler(()))(boolean)(executionContext)
    }

    implicit def tryToStepOps[A](tryValue: Try[A]): StepOps[A, Throwable] = new StepOps[A, Throwable] {
      override def orFailWith(failureHandler: (Throwable) => Result) = fromTry(failureHandler)(tryValue)(executionContext)
    }

    implicit def stepToResult[R <: Result](step: Step[R]): Future[Result] = step.run.map(_.merge)(executionContext)

    implicit def stepToEither[A](step: Step[A]): Future[Either[Result, A]] = step.run
  }
}
