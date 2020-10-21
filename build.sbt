inThisBuild(List(
  organization := "io.kanaka",
  homepage  := Some(url("https://github.com/Kanaka-io/play-monadic-actions")),
  description := "Mini DSL to allow the writing of Play! actions using for-comprehensions",
  licenses  := List(("Apache2", url("http://www.apache.org/licenses/LICENSE-2.0.txt"))),
  scalaVersion := "2.13.3",
  scalacOptions ++= Seq(
    "-unchecked",
    "-deprecation",
    "-feature",
    "-encoding", "utf8",
    "-Xfatal-warnings"
  ),
  developers := List(
    Developer(
      "vil1",
      "Valentin Kasas",
      "valentin.kasas@gmail.com",
      url("https://github.com/vil1")
    )
  )
))

crossScalaVersions := Seq("2.11.12", "2.12.12", "2.13.3")


val commonSettings = Seq (
  resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
  libraryDependencies ++= Seq(
    "com.typesafe.play" %% "play" % "2.7.3" % "provided",
    "com.typesafe.play" %% "play-test" % "2.7.3" % "test",
    "org.scalacheck" %% "scalacheck" % "1.14.1" % "test",
    "org.specs2" %% "specs2-core" % "4.7.1" % "test",
    "org.specs2" %% "specs2-scalacheck" % "4.7.1" % "test",
    "com.typesafe.play" %% "play-specs2" % "2.7.3" % "test" 
  )

)

def scalazCompatModuleSettings(moduleName: String, base: String, scalazVersion: String) = commonSettings ++ Seq(
  name := moduleName,
  libraryDependencies ++= Seq("org.scalaz" %% "scalaz-core" % scalazVersion),
  target := baseDirectory.value / ".." / base / "target"
)

def scalazCompatModule(id: String, moduleName: String, scalazVersion: String) = Project(id = id, base = file("scalaz"))
  .settings(scalazCompatModuleSettings(moduleName, id, scalazVersion):_*)
  .dependsOn(core % "compile->compile;test->test")

lazy val core = (project in file("core"))
  .settings(commonSettings:_*)
  .settings(name := "play-monadic-actions")

lazy val scalaz72 = scalazCompatModule(id = "scalaz72", moduleName = "play-monadic-actions-scalaz_7.2", scalazVersion = "7.2.28")

lazy val cats = (project in file("cats"))
  .settings(commonSettings:_*)
  .settings(
    name := "play-monadic-actions-cats",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "2.0.0" % "provided"
    )
  )
  .dependsOn(core % "compile->compile;test->test")


publishArtifact in Test := false

