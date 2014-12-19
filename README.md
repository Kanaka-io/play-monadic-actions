Play monadic actions
====================

This little play module provides some syntactic sugar that allows boilerplate-free Actions using for-comprehensions.

The [slides](https://kanaka-io.github.io/play-monadic-actions/index.html) (in french) explain in greater detail the problem
 that this project addresses, and how to use the solution in your own projects.

## Installation

The project is not released as a library yet, to use it in your play project you'll need to add something like the following in your sbt configuration :

~~~scala
lazy val playMonadicActions = RootProject(uri("https://github.com/Kanaka-io/play-monadic-actions.git"))

lazy val myProject = RootProject(file(".")).dependsOn(playMonadicActions)
~~~


## Usage

The DSL adds the `?|` operator to most of the types one could normally encounter in an action
(such as `Future[A]`, `Future[Option[A]]`, `Either[B,A]`, etc...). Given a function (or thunk) that transforms the error case in Result,
the `?|` operator will return an `EitherT[Future, Result, A]` (which is aliased to `Step[A]` for convenience)
enabling the writing of the whole action as a single for-comprehension.

~~~scala
object TestController extends Controller {

  import ActionDSL.Implicits._
  import play.api.libs.concurrent.Execution.Implicits.defaultContext

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

## Pitfalls

If you use the implicit conversions from ActionDSL.Implicits and the ?| operator on methods returning future,
you'll have to make sure that you have an implicit ExecutionContext available in scope. Otherwise, implicit conversion
from `Future` to `Step` will silently fail, and you'll probably end up with cryptic compilation errors.

## Credits

This project is widely inspired from the [play-monad-transformers activator template](https://github.com/lunatech-labs/play-monad-transformers#master) by Lunatech.