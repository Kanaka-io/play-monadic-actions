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
import play.api.libs.json.JsResult
import play.api.mvc.Result

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/**
  * @author Valentin Kasas
  */

trait MonadicActions {

  import scala.language.implicitConversions

  val executionContext: ExecutionContext = _root_.play.api.libs.concurrent.Execution.defaultContext

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
