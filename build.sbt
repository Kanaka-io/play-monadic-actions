name := """play-monadic-actions"""

version := "1.0.1-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  "org.scalaz" %% "scalaz-core" % "7.0.6"
)

organization := "io.kanaka"

description := "Mini DSL to allow the writing of Play! actions using for-comprehensions"

publishMavenStyle := true

licenses += ("Apache2", url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

homepage := Some(url("https://github.com/Kanaka-io/play-monadic-actions"))

pomExtra := <scm>
  <url>git@github.com:Kanaka-io/play-monadic-actions.git</url>
  <connection>scm:git:git@github.com:Kanaka-io/play-monadic-actions.git</connection>
</scm>
  <developers>
    <developer>
      <id>vkasas</id>
      <name>Valentin Kasas</name>
      <url>https://twitter.com/ValentinKasas</url>
    </developer>
  </developers>

publishArtifact in Test := false

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}