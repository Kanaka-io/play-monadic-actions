version in ThisBuild := "2.0.0-SNAPSHOT"

val commonSettings = Seq (
  resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
  libraryDependencies ++= Seq(
    "org.scalacheck" %% "scalacheck" % "1.13.0" % "test",
    "org.specs2" %% "specs2-core" % "3.7" % "test",
    "org.specs2" %% "specs2-junit" % "3.7" % "test",
    "com.typesafe.play" %% "play-specs2" % "2.4.6" % "test" excludeAll ExclusionRule(organization = "org.specs2")

  )
)

lazy val core = (project in file("core")).enablePlugins(PlayScala)
  .settings(commonSettings:_*)
  .settings(name := "play-monadic-actions")

def scalazCompatModuleSettings(moduleName: String, scalazVersion: String, output: File) = commonSettings ++ Seq(
    name := moduleName,
    libraryDependencies ++= Seq("org.scalaz" %% "scalaz-core" % scalazVersion),
    target := output
  )

lazy val scalaz71 = (project in file("scalaz"))
  .settings(scalazCompatModuleSettings("play-monadic-actions-scalaz_7.1", "7.1.0", file("scalaz71/target")))
  .dependsOn(core)
  .enablePlugins(PlayScala)

lazy val scalaz72 = (project in file("scalaz"))
  .settings(scalazCompatModuleSettings("play-monadic-actions-scalaz_7.2", "7.2.0", file("scalaz72/target")))
  .dependsOn(core)
  .enablePlugins(PlayScala)


lazy val cats = (project in file("cats"))
  .settings(commonSettings:_*)
  .settings(
    name := "play-monadic-actions-cats",
    libraryDependencies ++= Seq("org.typelevel" %% "cats" % "0.4.1")
  )
  .dependsOn(core)
  .enablePlugins(PlayScala)


scalaVersion in ThisBuild := "2.11.8"


organization in ThisBuild := "io.kanaka"

description := "Mini DSL to allow the writing of Play! actions using for-comprehensions"

publishMavenStyle in ThisBuild := true

licenses in ThisBuild += ("Apache2", url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

homepage in ThisBuild := Some(url("https://github.com/Kanaka-io/play-monadic-actions"))

pomExtra in ThisBuild := <scm>
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

publishTo in ThisBuild := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}
