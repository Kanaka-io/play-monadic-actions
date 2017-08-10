package io.kanaka.monadic.dsl

import org.scalacheck.{Prop, Properties}
import compat.scalaz.stepFunctor

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * @author Andrew Adams
  */
object ScalazFunctorSpec extends Properties("scalaz.Functor[Step]") with ArbitrarySteps {

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
