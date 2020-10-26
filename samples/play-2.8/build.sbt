name := """play-2.8"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.3"

libraryDependencies ++= Seq(
  guice,
  jdbc,
  caffeine,
  ws,
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % Test,
  "io.kanaka" %% "play-monadic-actions" % "2.2.0-SNAPSHOT",
  "io.kanaka" %% "play-monadic-actions-cats" % "2.2.0-SNAPSHOT",
  "org.typelevel" %% "cats-core" % "2.0.0"

)

