Play monadic actions
====================

[![Build Status](https://travis-ci.org/Kanaka-io/play-monadic-actions.svg?branch=master)](https://travis-ci.org/Kanaka-io/play-monadic-actions)

This little play module provides some syntactic sugar that allows boilerplate-free Actions using for-comprehensions.

The [slides](https://kanaka-io.github.io/play-monadic-actions/index.html) (in french) explain in greater detail the problem
 that this project addresses, and how to use the solution in your own projects.

## Installation

Using sbt :

~~~scala
libraryDependencies += "io.kanaka" %% "play-monadic-actions" % "1.0"
~~~


## Usage

The DSL adds the `?|` operator to most of the types one could normally encounter in an action
(such as `Future[A]`, `Future[Option[A]]`, `Either[B,A]`, etc...). Given a function (or thunk) that transforms the error case in `Result`,
the `?|` operator will return an `EitherT[Future, Result, A]` (which is aliased to `Step[A]` for convenience)
enabling the writing of the whole action as a single for-comprehension.

~~~scala
package controllers

import ActionDSL.MonadicAction
object TestController extends Controller with MonadicActions {

  implicit val StatusUpdateFormat = Json.format[StatusUpdate]

  def action1(userId: String) = Action.async(parse.json) {
    request =>
      for {
        su   <- request.body.validate[StatusUpdate] ?| BadRequest(JsError.toFlatJson(_:ActionDSL.JsErrorContent))
        user <- SomeService.findUser(userId)        ?| NotFound
        _    <- SomeService.performUpdate(user, su) ?| Conflict
      } yield NoContent
  }
}
~~~

## Credits

This project is widely inspired from the [play-monad-transformers activator template](https://github.com/lunatech-labs/play-monad-transformers#master) by Lunatech.
