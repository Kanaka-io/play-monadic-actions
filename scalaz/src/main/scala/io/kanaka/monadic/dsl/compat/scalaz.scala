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
package io.kanaka.monadic.dsl.compat

import io.kanaka.monadic.dsl.{Step, StepOps}
import play.api.mvc.Result

import _root_.scalaz.{EitherT, Functor, Monad, Validation, \/, OptionT}
import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions

/**
  * @author Valentin Kasas
  */
trait ScalazToStepOps {

  // We could use `scalaz.std.scalaFuture.futureInstance` here, but doing so seems to mess things up in scalaz 7.1.x
  // preventing us to use the same code for 7.1.x and 7.2.x.
  // We must use this explicitly though, in order to not mess with our client's implicit scope
  private[this] def futureFunctor(
      implicit ec: ExecutionContext): Functor[Future] = new Functor[Future] {
    override def map[A, B](fa: Future[A])(f: (A) => B): Future[B] = fa map f
  }

  implicit def disjunctionToStep[A, B](disjunction: B \/ A)(
      implicit ec: ExecutionContext): StepOps[A, B] = new StepOps[A, B] {
    override def orFailWith(failureHandler: (B) => Result): Step[A] =
      Step(Future.successful(disjunction.leftMap(failureHandler).toEither))
  }

  implicit def validationToStep[A, B](validation: Validation[B, A])(
      implicit ec: ExecutionContext): StepOps[A, B] = new StepOps[A, B] {
    override def orFailWith(failureHandler: (B) => Result): Step[A] =
      Step(
          Future.successful(
              validation.fold(failureHandler andThen Left.apply, Right.apply)))
  }

  implicit def eithertFutureToStep[A, B](eithertFuture: EitherT[Future, B, A])(
      implicit ec: ExecutionContext) = new StepOps[A, B] {
    private[this] val functor = futureFunctor(ec)
    override def orFailWith(failureHandler: (B) => Result): Step[A] =
      Step(eithertFuture.leftMap(failureHandler)(functor).toEither(functor))
  }

  implicit def optiontFutureToStep[A](optiontFuture: OptionT[Future, A])(
      implicit ec: ExecutionContext) = new StepOps[A, Unit] {
    override def orFailWith(failureHandler: (Unit) => Result): Step[A] =
      Step(
          optiontFuture.fold(Right(_), Left(failureHandler(())))(
              futureFunctor))
  }
  implicit def futureDisjunctionToStep[A, B](futureDisj: Future[B \/ A])(
      implicit ec: ExecutionContext) = new StepOps[A, B] {
    override def orFailWith(failureHandler: (B) => Result): Step[A] =
      Step(futureDisj.map(_.leftMap(failureHandler).toEither))
  }

  implicit def futureValidationToStep[A, B](
      futureValid: Future[Validation[B, A]])(
      implicit ec: ExecutionContext): StepOps[A, B] = new StepOps[A, B] {
    override def orFailWith(failureHandler: (B) => Result): Step[A] =
      Step(
          futureValid.map(
              _.fold(failureHandler andThen Left.apply, Right.apply)))
  }

}

trait ScalazStepInstances {

  implicit def stepFunctor(implicit ec: ExecutionContext): Functor[Step] =
    new Functor[Step] {
      override def map[A, B](fa: Step[A])(f: (A) => B): Step[B] = fa map f
    }

  implicit def stepMonad(implicit ec: ExecutionContext): Monad[Step] =
    new Monad[Step] {
      override def bind[A, B](fa: Step[A])(f: (A) => Step[B]): Step[B] =
        fa flatMap f

      override def point[A](a: => A): Step[A] = Step.unit(a)
    }

}

object scalaz extends ScalazStepInstances with ScalazToStepOps
