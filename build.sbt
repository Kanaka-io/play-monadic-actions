
name := """play-monadic-actions"""

version := "1.1.4"

lazy val root = project in file(".")

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  "org.scalaz" %% "scalaz-core" % "7.1.3",
  "com.typesafe.play" %% "play" % "2.5.2" % "provided"
)

organization := "io.kanaka"

description := "Mini DSL to allow the writing of Play! actions using for-comprehensions"

licenses +=("Apache2", url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

// To publish, put these credentials in ~/.ivy2/credentials
//credentials += Credentials("Sonatype Nexus Repository Manager", "publish-nexus.agiledigital.com.au", "****", "****"),
credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

publishTo := {
  val nexus = "http://publish-nexus.agiledigital.com.au/nexus/"
  if (version.value.trim.endsWith("SNAPSHOT")) {
    Some("snapshots" at nexus + "content/repositories/snapshots")
  } else {
    Some("releases" at nexus + "content/repositories/releases")
  }
}
