package io.kanaka.monadic.dsl

import org.scalacheck.{Prop, Properties}
import io.kanaka.monadic.dsl.compat.cats.stepFunctor

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * @author Andrew Adams
  */
object CatsStepFunctorSpecification extends Properties("cats.Functor[Step]") with ArbitrarySteps {

  property("identity") = Prop.forAll { (int: Int) =>
    val left = stepFunctor.map(Step.unit[Int](int))(identity)
    val right = Step.unit[Int](int)
    Await.result(left, 1.second) == Await.result(right.run, 1.second)
  }

  property("associative") = Prop.forAll { (int: Int, f: Int => Int, g: Int => Int) =>
    val left = stepFunctor.map(stepFunctor.map(Step.unit[Int](int))(f))(g)
    val right = stepFunctor.map(Step.unit[Int](int))(g compose f)
    Await.result(left, 1.second) == Await.result(right.run, 1.second)
  }

}
