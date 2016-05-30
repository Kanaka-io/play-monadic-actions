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

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Prop._
import org.scalacheck.{Arbitrary, Gen, Prop, Properties}
import play.api.mvc.{Result, Results}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * @author Valentin Kasas
  */
object StepSpecification extends Properties("Step") {


  implicit def arbitraryResult: Arbitrary[Result] = Arbitrary(
    Gen.oneOf(Results.NotFound, Results.NoContent, Results.Ok, Results.InternalServerError, Results.BadGateway)
  )

  implicit def arbitraryStepA[A](implicit arbA: Arbitrary[A], arbResult: Arbitrary[Result]): Arbitrary[Step[A]] = Arbitrary(
    for {
    isLeft <- arbitrary[Boolean]
    a <- arbitrary[A](arbA)
    result <- arbitrary[Result](arbResult)
  } yield {
    if (isLeft) {
      Step(Future.successful(Left(result)))
    } else {
      Step(Future.successful(Right(a))
      )
    }
  })


  property("left identity") = Prop.forAll{ (int: Int , f: Int => Step[String] )  =>
    val l = Step.unit[Int](int) flatMap f
    val r = f(int)

    Await.result(l.run, 1.second) == Await.result(r.run, 1.second)
  }

  property("right identity") = Prop.forAll{ ( step: Step[String] )  =>
    val l = step flatMap Step.unit
    val r = step

    Await.result(l.run, 1.second) == Await.result(r.run, 1.second)
  }

  property("associativity") = Prop.forAll{ (step: Step[Int], f: Int => Step[String], g: String => Step[Boolean]) =>
    val l = (step flatMap f) flatMap g
    val r = step flatMap(x => f(x) flatMap g)

    Await.result(l.run, 1.second) == Await.result(r.run, 1.second)
  }
}
