package io.kanaka.play

import play.api.mvc.{Result, Results}

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author Valentin Kasas
  */
final case class Step[A](run: Future[Either[Result, A]], executionContext: ExecutionContext) {

  def map[B](f : A => B) = copy(run = run.map(_.right.map(f))(executionContext))

  def flatMap[B](f: A => Step[B]) = copy(run = run.flatMap(_.fold(err => Future.successful(Left[Result, B](err)), succ => f(succ).run))(executionContext))

  def withFilter(p: A => Boolean): Step[A] = copy(run = run.filter {
    case Right(a) if p(a) => true
  }(executionContext))

}
