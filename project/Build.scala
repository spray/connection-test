import sbt._
import Keys._
import cc.spray.revolver.RevolverPlugin._

object Build extends sbt.Build {
  import Dependencies._

  lazy val project = Project("connection-test", file("."))
    .settings(Revolver.settings: _*)
    .settings(
      organization  := "cc.spray",
      version       := "1.0",
      scalaVersion  := "2.9.1",
      scalacOptions := Seq("-deprecation", "-encoding", "utf8"),
      resolvers     ++= Dependencies.resolutionRepos,
      libraryDependencies ++=
        compile(akkaActor, sprayCan) ++
        runtime(akkaSlf4j, slf4j, logback)
    )
}

