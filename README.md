Play monadic actions
====================

[![Build Status](https://travis-ci.org/Kanaka-io/play-monadic-actions.svg?branch=master)](https://travis-ci.org/Kanaka-io/play-monadic-actions) [![Gitter chat](https://badges.gitter.im/Kanaka-io/play-monadic-actions.png)](https://gitter.im/Kanaka-io/play-monadic-actions "Gitter chat") [![Coverage Status](https://coveralls.io/repos/github/Kanaka-io/play-monadic-actions/badge.svg?branch=master)](https://coveralls.io/github/Kanaka-io/play-monadic-actions?branch=master) 
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.kanaka/play-monadic-actions_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.kanaka/play-monadic-actions_2.11)

This little play module provides some syntactic sugar that allows boilerplate-free Actions using for-comprehensions.

## Motivation

It is commonly admitted that controllers should be lean and only focus on parsing an incoming HTTP request, call (possibly many) service methods and finally build an HTTP response (preferably with a proper status). In the context of an asynchronous framework like Play!, most of these operations results are (or can be) wrapped in a `Future`, and since their outcome can be either positive or negative, these results have a type that is more or less isomorphic to `Future[Either[X, Y]]`. 
 
This matter of facts raises some readability issues. Consider for example the following action : 

~~~scala
class ExampleController extends Controller {
  
  val beerOrderForm: Form[BeerOrder] = ???
  def findAdultUser(id: String): Future[Either[UnderageError, User]] = ???
  def sellBeer(beerName: String, customer: User): Future[Either[OutOfStockError, Beer]] = ???
  
  def orderBeer() = Action.async {
     beerOrderForm.bindFromRequest().fold(
       formWithErrors => BadRequest(views.html.orderBeer(formWithErrors),
       beerOrder => 
        findAdultUser(beerOrder.userId).map(
          _.fold(
            ue => Conflict(displayError(ue)),
            user => 
              sellBeer(beerOrder.beerName, user).map(
                _.fold(
                  oose => NotFound(displayError(oose)),
                  beer => Ok(displayBeer(beer)  
                )
              )    
          )
        )
  }
}
~~~

This is pretty straightforward, and yet the different *steps* of the computation are not made very clear. And since I've typed this in a regular text editor with no syntax highlighting nor static code analysis, there is an obvious error that you may not have spotted (there's a `map` instead of a `flatMap` somewhere). 

This library addresses this problem by defining a `Step[A]` monad, which is roughly a `Future[Either[Result, A]]`, but with a right bias on the `Either` part, and providing a little DSL to lift relevant types into this monad's context.

Using it, the previous example becomes :

~~~scala
import io.kanaka.monadic.dsl._

// don't forget to import an implicit ExecutionContext
import play.api.libs.concurrent.Execution.Implicits.defaultContext 

class ExampleController extends Controller {
  
  val beerOrderForm: Form[BeerOrder] = ???
  def findAdultUser(id: String): Future[Either[UnderageError, User]] = ???
  def sellBeer(beerName: String, customer: User): Future[Either[OutOfStockError, Beer]] = ???
  
  def orderBeer() = Action.async {
    for {
      beerOrder <- beerOrderForm.bindFromRequest()    ?| (formWithErrors => BadRequest(views.html.orderBeer(formWithErrors))
      user      <- findAdultUser(beerOrder.userId)    ?| (ue => Conflict(displayError(ue))
      beer      <- sellBeer(beerOrder.beerName, user) ?| (oose => NotFound(displayError(oose))  
    } yield Ok(displayBeer(beer))
  }
}
~~~

**IMPORTANT NOTE** : one **MUST** provide an implicit `ExecutionContext` for the DSL to work

## How it works

The DSL introduces the binary `?|` operator. The happy path goes on the left hand side of the operator and the error path goes on the right : `happy ?| error`. Such expression produces a `Step[A]` which has all the required methods to make it usable in a for-comprehension. 

So for example, if a service methods `foo`returns a `Future[Option[A]]`, we assume the happy path to be the case where the `Future` succeeds with a `Some[A]` and the error path to be the case where it succeeds with a `None` (the case where the `Future` fails is already taken care of by play's error handler). So we need to provide a proper `Result` to be returned in the error case (most probably a `NotFound`) and then we can write  

~~~scala
for {
 // ...
 a <- foo ?| NotFound    
 // ...
} yield {
 // ...
}
~~~

The `a` here would be of type `A`, meaning that we've extracted the meaningful value from the `Future[Option[A]]` return by `foo`.
Of course, if `foo` returns a `Future[None]` the for-comprehension is not evaluated further, and returns `NotFound`.

The right hand side of the `?|` operator (the error management part) is a function (or a thunk) that must return a `Result` and whose input type depends of the type of the expression on the left hand side of the operator (see the table of supported conversions below).

## Filtering and Pattern-matching

`Step[_]` defines a `withFilter` method, which means that one can use pattern matching and filtering in for-comprehensions involving `Step[_]`.

For example, if `bar` is of type `Future[Option[(Int, String)]]`, one can write 

~~~scala
for {
 (i, s) <- bar ?| NotFound if s.length >= i
} yield Ok(s.take(i))
~~~

Please note though that in the case where the predicate `s.length >= i` does not hold, the whole `Future` will fail with a `NoSuchElementException`, and there is no easy way to transform this failure into a user-specified `Result`.

## Supported conversions

The DSL supports the following conversions : 

| Defining module | Source type | Type of the right hand side | Type of the extracted value |
| --- | --- | --- | --- |
| `play-monadic-actions` | `Boolean` | `=> Result` | `Unit` | 
| `play-monadic-actions` | `Option[A]` | `=> Result` | `A` |
| `play-monadic-actions` | `Try[A]` | `Throwable => Result` | `A` |
| `play-monadic-actions` | `Either[B, A]` | `B => Result` | `A` |
| `play-monadic-actions` | `Form[A]` | `Form[A] => Result` | `A` |
| `play-monadic-actions` | `JsResult[A]` | `Seq[(JsPath, Seq[ValidationError])] => Result`| `A` |
| `play-monadic-actions` | `Future[A]` | `Throwable => Result` | `A` |
| `play-monadic-actions` | `Future[Boolean]` | `=> Result` | `Unit` |
| `play-monadic-actions` | `Future[Option[A]]` | `=> Result` | `A` |
| `play-monadic-actions` | `Future[Either[B, A]]` | `B => Result` | `A` |
| `play-monadic-actions-cats` | `B Xor A` | `B => Result` | `A` |
| `play-monadic-actions-cats` | `Future[B Xor A]` | `B => Result` | `A` |
| `play-monadic-actions-cats` | `XorT[Future, B, A]` | `B => Result` | `A` |
| `play-monadic-actions-cats` | `OptionT[Future, A]` | `=> Result` | `A` |
| `play-monadic-actions-cats` | `Validated[B Xor A]` | `B => Result` | `A` |
| `play-monadic-actions-cats` | `Future[Validated[B Xor A]]` | `B => Result` | `A` |
| `play-monadic-actions-scalaz-7-1`  | `B \/ A` |  `B => Result` | `A` |
| `play-monadic-actions-scalaz-7-1` | `Future[B \/ A]` |  `B => Result` | `A` |
| `play-monadic-actions-scalaz-7-1` | `Validation[B, A]` |  `B => Result` | `A` |
| `play-monadic-actions-scalaz-7-1` | `EitherT[Future, B, A]` |  `B => Result` | `A` |
| `play-monadic-actions-scalaz-7-1` | `OptionT[Future, A]` |  `Unit => Result` | `A` |
| `play-monadic-actions-scalaz-7-1` | `Future[Validation[B, A]]` |  `B => Result` | `A` |
| `play-monadic-actions-scalaz-7-2` | `B \/ A` |  `B => Result` | `A` |
| `play-monadic-actions-scalaz-7-2` | `Future[B \/ A]` |  `B => Result` | `A` |
| `play-monadic-actions-scalaz-7-2` | `Validation[B, A]` |  `B => Result` | `A` |
| `play-monadic-actions-scalaz-7-2` | `EitherT[Future, B, A]` |  `B => Result` | `A` |
| `play-monadic-actions-scalaz-7-2` | `OptionT[Future, A]` |  `Unit => Result` | `A` |
| `play-monadic-actions-scalaz-7-2` | `Future[Validation[B, A]]` |  `B => Result` | `A` |


## Installation

Using sbt :

Current version is 2.1.0
~~~scala
libraryDependencies += "io.kanaka" %% "play-monadic-actions" % "2.1.0"
~~~

There are also contrib modules for interoperability with scalaz and cats : 

|module name|is compatible with / built against|
| --- | --- |
|play-monadic-actions-cats| cats 0.7.2|
|play-monadic-actions-scalaz_7-1| scalaz 7.1.8|
|play-monadic-actions-scalaz_7-2| scalaz 7.2.3|

Each of these module provides `Functor` and `Monad` instances for `Step[_]` as well as conversions for relevant types in the target library 

These instances and conversions are made available by importing `io.kanaka.monadic.dsl.compat.cats._` and `io.kanaka.monadic.dsl.compat.scalaz._` respectively.
 
## Compatibility

- Version `2.1.0` is compatible with Play! `2.6.x`
- Version `2.0.0` is compatible with Play! `2.5.x`
- Version `1.1.0` is compatible with Play! `2.4.x`
- Version `1.0.1` is compatible with Play! `2.3.x`

From version `2.0.0` up, dependencies toward play and cats are defined as `provided`, meaning that you can use the DSL along with any version of these projects you see fit. The sample projects under `samples/` demonstrate this capability.

From version `2.1.0` up, the modules are published for scala `2.11` and `2.12`. Previous versions are only published for scala `2.11`. 

## Contributors

[Valentin Kasas](https://twitter.com/ValentinKasas)

[Damien Gouyette](https://twitter.com/cestpasdur)

[David R. Bild](https://github.com/drbild)

[Bjørn Madsen](https://github.com/aeons)

[Christophe Calves](https://github.com/christophe-calves)

[Maxim Karpov](https://github.com/makkarpov)

[Richard Searle](https://github.com/searler)

[Andrew Adams](https://github.com/adamsar)

... your name here

## Credits

This project is widely inspired from the [play-monad-transformers activator template](https://github.com/lunatech-labs/play-monad-transformers#master) by Lunatech.

It also uses [coursier](https://github.com/alexarchambault/coursier) to fetch dependencies in parallel, which is a pure bliss. Take a look if you don't know it yet. 
