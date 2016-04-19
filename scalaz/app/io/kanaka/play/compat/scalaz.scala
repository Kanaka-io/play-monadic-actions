package io.kanaka.play.compat

import io.kanaka.play.{Step, StepOps}
import play.api.mvc.Result
import _root_.scalaz.{\/, Validation, Functor, Monad}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions
/**
  * @author Valentin Kasas
  */
trait ScalazToStepOps {

  implicit def disjunctionToStep[A, B](disjunction: B \/ A)(implicit ec: ExecutionContext): StepOps[A, B] = new StepOps[A, B] {
    override def orFailWith(failureHandler: (B) => Result): Step[A] = Step(Future.successful(disjunction.leftMap(failureHandler).toEither), ec)
  }

  implicit def validationToStep[A, B](validation: Validation[B, A])(implicit ec: ExecutionContext): StepOps[A, B] = new StepOps[A, B] {
    override def orFailWith(failureHandler: (B) => Result): Step[A] = Step(Future.successful(validation.fold(failureHandler andThen Left.apply, Right.apply)), ec)
  }

  implicit def futureDisjunctionToStep[A, B](futureDisj: Future[B \/ A])(implicit ec: ExecutionContext) = new StepOps[A, B] {
    override def orFailWith(failureHandler: (B) => Result): Step[A] = Step(futureDisj.map(_.leftMap(failureHandler).toEither), ec)
  }

  implicit def futureValidationToStep[A, B](futureValid: Future[Validation[B,A]])(implicit ec: ExecutionContext): StepOps[A, B] = new StepOps[A, B] {
    override def orFailWith(failureHandler: (B) => Result): Step[A] = Step(futureValid.map(_.fold(failureHandler andThen Left.apply, Right.apply)), ec)
  }

}

trait ScalazStepInstances {

  implicit val stepFunctor: Functor[Step] = new Functor[Step] {
    override def map[A, B](fa: Step[A])(f: (A) => B): Step[B] = fa map f
  }

  implicit val stepMonad: Monad[Step] = new Monad[Step] {
    override def bind[A, B](fa: Step[A])(f: (A) => Step[B]): Step[B] = fa flatMap f

    override def point[A](a: => A): Step[A] = Step.unit(a)
  }

}

object scalaz extends ScalazStepInstances with ScalazToStepOps
