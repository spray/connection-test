import sbt._

object Dependencies {
  val resolutionRepos = Seq(
    ScalaToolsSnapshots,
    "Typesafe repo" at "http://repo.typesafe.com/typesafe/releases/",
    "spray repo" at "http://repo.spray.cc/"
  )

  def compile   (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "compile")
  def provided  (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "provided")
  def test      (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "test")
  def runtime   (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "runtime")
  def container (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "container")

  object V {
    val akka     = "2.0"
    val spray    = "1.0-M1"
  }

  val akkaActor   = "com.typesafe.akka" %  "akka-actor"      % V.akka
  val akkaSlf4j   = "com.typesafe.akka" %  "akka-slf4j"      % V.akka
  val sprayCan    = "cc.spray"          %  "spray-can"       % V.spray
  val logback     = "ch.qos.logback"    %  "logback-classic" % "1.0.0"
  val slf4j       = "org.slf4j"         %  "slf4j-api"       % "1.6.4"
}