Play monadic actions
====================

[![Build Status](https://travis-ci.org/Kanaka-io/play-monadic-actions.svg?branch=master)](https://travis-ci.org/Kanaka-io/play-monadic-actions)

This little play module provides some syntactic sugar that allows boilerplate-free Actions using for-comprehensions.

The [slides](https://kanaka-io.github.io/play-monadic-actions/index.html) (in french) explain in greater detail the problem
 that this project addresses, and how to use the solution in your own projects.

## Installation

Using sbt :

Last version is 1.1.0
~~~scala
libraryDependencies += "io.kanaka" %% "play-monadic-actions" % "1.1.0"
~~~

## Compatibility

- Version `1.0.1` is compatible with Play! `2.3.x`
- Version `1.1.0` is compatible with Play! `2.4.x`

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

## Caveats

### ExecutionContext

For compatibility reasons, play-monadic-actions depends on scalaz version 7.0.6 that does not provide `Monad` or `Functor` instances for `scala.concurrent.Future`.
Therefore, those instances have to be provided locally. To be able to use `map` on `Future`s and than provide those instances, an `ExecutionContext` must be selected.
In order to keep the DSL simple and yet allow one to use a specific `ExecutionContext`, `Monad[Future]` and `Functor[Future]` instances are defined as `val`s inside
trait `MonadicActions` (meaning that each controller extending `MonadicActions` has its own version of theses instances). These instances explicitly use the value of
the the local val `executionContext` that defaults to `play.api.libs.concurrent.Execution.defaultContext`. One can thus override this field on a controller to use another
execution context.

### Filtering / Pattern matching in for-comprehensions

Similarly, a `Monoid[Result]` instance is required to enable filtering/pattern-matching in for-comprehensions on `EitherT[Future, Result, X]`. The problem is that
`Result` doesn't really have a monoidal structure. The provided instance will systematically yield an `InternalServerError` when a filter predicate does not hold
or if an extracted value does not match the specified pattern. Therefore, this feature should be used with caution. For example the following :

~~~scala
def changePassword() = Action.async {
  implicit request =>
    for {
      (password, confirmation) <- passwordForm.bindFromRequest ?| (formWithErrors => BadRequest(formWithErrors.errorsAsJson)
      _ <- (password == confirmation) ?| BadRequest("the two passwords must match")
    } yield Ok
}
~~~

is preferable to the more straightforward :

~~~scala
def changePassword() = Action.async {
  implicit request =>
    for {
      (password, confirmation) <- passwordForm.bindFromRequest ?| (formWithErrors => BadRequest(formWithErrors.errorsAsJson) if password == confirmation
    } yield Ok
}
~~~

which would yield an `InternalServerError` if the two passwords don't match.

## Contributors

[Valentin Kasas](https://twitter.com/ValentinKasas)

[Damien Gouyette](https://twitter.com/cestpasdur)

[David R. Bild](https://github.com/drbild)

... your name here

## Credits

This project is widely inspired from the [play-monad-transformers activator template](https://github.com/lunatech-labs/play-monad-transformers#master) by Lunatech.
