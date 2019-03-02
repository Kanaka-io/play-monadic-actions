scalaVersion in ThisBuild := "2.12.8"

organization in ThisBuild := "io.kanaka"

description := "Mini DSL to allow the writing of Play! actions using for-comprehensions"

licenses in ThisBuild += ("Apache2", url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

homepage in ThisBuild := Some(url("https://github.com/Kanaka-io/play-monadic-actions"))

scalacOptions in ThisBuild ++= Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-encoding", "utf8",
  "-Xfatal-warnings"
)

crossScalaVersions := Seq("2.11.11", "2.12.8")


val commonSettings = Seq (
  resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
  libraryDependencies ++= Seq(
    "com.typesafe.play" %% "play" % "2.6.2" % "provided",
    "com.typesafe.play" %% "play-test" % "2.6.2" % "test",
    "org.scalacheck" %% "scalacheck" % "1.13.5" % "test",
    "org.specs2" %% "specs2-core" % "3.9.4" % "test",
    "org.specs2" %% "specs2-scalacheck" % "3.9.4" % "test",
    "com.typesafe.play" %% "play-specs2" % "2.6.2" % "test" 
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

lazy val scalaz71 = scalazCompatModule(id = "scalaz71", moduleName = "play-monadic-actions-scalaz_7.1", scalazVersion = "7.1.14")

lazy val scalaz72 = scalazCompatModule(id = "scalaz72", moduleName = "play-monadic-actions-scalaz_7.2", scalazVersion = "7.2.14")

lazy val cats = (project in file("cats"))
  .settings(commonSettings:_*)
  .settings(
    name := "play-monadic-actions-cats",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "1.6.0" % "provided"
    )
  )
  .dependsOn(core % "compile->compile;test->test")



publishMavenStyle in ThisBuild := true

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

releasePublishArtifactsAction := com.typesafe.sbt.pgp.PgpKeys.publishSigned.value
