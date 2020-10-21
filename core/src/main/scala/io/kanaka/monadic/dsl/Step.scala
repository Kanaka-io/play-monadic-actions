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

import play.api.mvc.Result

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author Valentin Kasas
  */
final case class Step[+A](run: Future[Either[Result, A]]) {

  def map[B](f: A => B)(implicit ec: ExecutionContext): Step[B] =
    copy(run = run.map( _ match {
      case Left(err) => Left(err)
      case r @ Right(a) => r.copy(f(a))
    }))

  def flatMap[B](f: A => Step[B])(implicit ec: ExecutionContext): Step[B] =
    copy(run = run.flatMap(_.fold(err =>
                  Future.successful(Left[Result, B](err)), succ =>
                  f(succ).run)))

  def withFilter(p: A => Boolean)(implicit ec: ExecutionContext): Step[A] =
    copy(run = run.filter {
      case Right(a) if p(a) => true
      case Left(e) => true
      case _ => false
    })

}

object Step {

  def unit[A](a: A): Step[A] = Step(Future.successful(Right(a)))

}

trait StepOps[A, B] {
  def orFailWith(failureHandler: B => Result): Step[A]
  def ?|(failureHandler: B => Result): Step[A] = orFailWith(failureHandler)
  def ?|(failureThunk: => Result): Step[A] = orFailWith(_ => failureThunk)
}
