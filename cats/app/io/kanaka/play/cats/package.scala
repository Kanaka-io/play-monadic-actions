package io.kanaka.play


import _root_.cats.{Functor, Monad}
import _root_.cats.data.{Xor, Validated}
import play.api.mvc.Result

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions
/**
  * @author Valentin Kasas
  */
package object cats {

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

  implicit val stepFunctor: Functor[Step] = new Functor[Step] {
    override def map[A, B](fa: Step[A])(f: (A) => B): Step[B] = fa map f
  }

  implicit val stepMonad: Monad[Step] = new Monad[Step] {

    override def flatMap[A, B](fa: Step[A])(f: (A) => Step[B]): Step[B] = fa flatMap f

    override def pure[A](x: A): Step[A] = Step.unit(x)
  }
}
