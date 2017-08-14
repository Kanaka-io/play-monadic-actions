name := """play-2.6"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.3"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.1" % Test,
  "io.kanaka" %% "play-monadic-actions" % "2.0.2-SNAPSHOT",
  "io.kanaka" %% "play-monadic-actions-cats" % "2.0.2-SNAPSHOT",
  "org.typelevel" %% "cats-core" % "1.0.0-MF"

)

