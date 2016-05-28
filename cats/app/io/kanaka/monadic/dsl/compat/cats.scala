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

import _root_.cats.{Functor, Monad}
import _root_.cats.data.{Validated, Xor}
import io.kanaka.monadic.dsl.{Step, StepOps}
import play.api.mvc.Result

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions

/**
  * @author Valentin Kasas
  */
trait CatsToStepOps {

  implicit def xorToStep[A, B](xor: B Xor A)(implicit ec: ExecutionContext): StepOps[A, B] = new StepOps[A, B] {
    override def orFailWith(failureHandler: (B) => Result): Step[A] = Step(Future.successful(xor.leftMap(failureHandler).toEither), ec)
  }

  implicit def validatedToStep[A, B](validated: Validated[B, A])(implicit ec: ExecutionContext): StepOps[A, B] = new StepOps[A, B] {
    override def orFailWith(failureHandler: (B) => Result): Step[A] = Step(Future.successful(validated.leftMap(failureHandler).toEither), ec)
  }

  implicit def futureXorToStep[A, B](futureXor: Future[B Xor A])(implicit ec: ExecutionContext): StepOps[A, B] = new StepOps[A, B] {
    override def orFailWith(failureHandler: (B) => Result): Step[A] = Step(futureXor.map(_.leftMap(failureHandler).toEither), ec)
  }

  implicit def futureValidatedToStep[A, B](futureValidated: Future[Validated[B, A]])(implicit ec: ExecutionContext): StepOps[A, B] = new StepOps[A, B] {
    override def orFailWith(failureHandler: (B) => Result): Step[A] = Step(futureValidated.map(_.leftMap(failureHandler).toEither)(ec), ec)
  }

}

trait CatsStepInstances {

  implicit val stepFunctor: Functor[Step] = new Functor[Step] {
    override def map[A, B](fa: Step[A])(f: (A) => B): Step[B] = fa map f
  }

  implicit val stepMonad: Monad[Step] = new Monad[Step] {

    override def flatMap[A, B](fa: Step[A])(f: (A) => Step[B]): Step[B] = fa flatMap f

    override def pure[A](x: A): Step[A] = Step.unit(x)
  }

}

object cats extends CatsStepInstances with CatsToStepOps
