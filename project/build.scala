import sbt._
import Keys._

object MyBuild extends Build {

  val name = "wikipedia-list-extraction"

  val version = "1.0"

  val scalaVersion = "2.11.6"

  scalacOptions += "-target:jvm-1.8"

  lazy val root: Project = project.in(file("."))
}