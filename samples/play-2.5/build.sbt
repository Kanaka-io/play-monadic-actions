name := """play-2.5"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
  "io.kanaka" %% "play-monadic-actions" % "2.0.0-RC2",
  "io.kanaka" %% "play-monadic-actions-cats" % "2.0.0-RC2",
  "org.typelevel" %% "cats" % "0.6.0"

)

