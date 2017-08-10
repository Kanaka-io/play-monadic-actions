package io.kanaka.monadic.dsl

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}
import play.api.mvc.{Result, Results}

import scala.concurrent.Future

trait ArbitrarySteps {

  implicit def arbitraryResult: Arbitrary[Result] = Arbitrary(
    Gen.oneOf(Results.NotFound,
      Results.NoContent,
      Results.Ok,
      Results.InternalServerError,
      Results.BadGateway)
  )

  implicit def arbitraryStepA[A](implicit arbA: Arbitrary[A],
                                 arbResult: Arbitrary[Result]): Arbitrary[Step[A]] =
    Arbitrary(for {
      isLeft <- arbitrary[Boolean]
      a <- arbitrary[A](arbA)
      result <- arbitrary[Result](arbResult)
    } yield {
      if (isLeft) {
        Step(Future.successful(Left(result)))
      } else {
        Step(Future.successful(Right(a)))
      }
    })

}