package io.kanaka.monadic.dsl

import io.kanaka.monadic.dsl.compat.cats.stepMonad

import scala.concurrent.ExecutionContext.Implicits.global
import cats.instances.all._
import org.scalacheck.{Prop, Properties}

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * @author Andrew Adams
  */
object CatsStepMonadSpecification extends Properties("cats.Monad[Step]") with ArbitrarySteps {

  property("left identity") = Prop.forAll { (int: Int, f: Int => Step[String]) =>
    val l = stepMonad.flatMap(stepMonad.pure[Int](int))(f)
    val r = f(int)

    Await.result(l, 1.second) == Await.result(r.run, 1.second)
  }

  property("right identity") = Prop.forAll { (step: Step[String]) =>
    val l = stepMonad.flatMap(step)(stepMonad.pure(_))
    val r = step

    Await.result(l.run, 1.second) == Await.result(r.run, 1.second)
  }

  property("associativity") = Prop.forAll { (step: Step[Int], f: Int => Step[String], g: String => Step[Boolean]) =>
    val l = stepMonad.flatMap(stepMonad.flatMap(step)(f))(g)
    val r = stepMonad.flatMap(step)(x => stepMonad.flatMap(f(x))(g))

    Await.result(l.run, 1.second) == Await.result(r.run, 1.second)
  }


}
