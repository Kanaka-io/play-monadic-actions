package io.kanaka.play

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Prop._
import org.scalacheck.{Arbitrary, Gen, Prop, Properties}
import play.api.mvc.{Result, Results}

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
      Step(Future.successful(Left(result)), scala.concurrent.ExecutionContext.global)
    } else {
      Step(Future.successful(Right(a)), scala.concurrent.ExecutionContext.global)
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
