package io.kanaka.monadic.dsl

import org.scalacheck.{Prop, Properties}
import compat.scalaz.stepMonad

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * @author Andrew Adams
  */
object ScalazStepMonadSpec extends Properties("scalaz.Monad[Step]") with ArbitrarySteps {

  property("left identity") = Prop.forAll { (int: Int, f: Int => Step[String]) =>
    val l = stepMonad.bind(stepMonad.pure[Int](int))(f)
    val r = f(int)

    Await.result(l, 1.second) == Await.result(r.run, 1.second)
  }

  property("right identity") = Prop.forAll { (step: Step[String]) =>
    val l = stepMonad.bind(step)(stepMonad.pure(_))
    val r = step

    Await.result(l.run, 1.second) == Await.result(r.run, 1.second)
  }

  property("associativity") = Prop.forAll { (step: Step[Int], f: Int => Step[String], g: String => Step[Boolean]) =>
    val l = stepMonad.bind(stepMonad.bind(step)(f))(g)
    val r = stepMonad.bind(step)(x => stepMonad.bind(f(x))(g))

    Await.result(l.run, 1.second) == Await.result(r.run, 1.second)
  }

}
