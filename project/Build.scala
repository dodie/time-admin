import sbt._
import sbt.Keys._

object TheBuild extends Build {
  lazy val root = Project("root", file("."))
    .configs(Configs.all: _*)
    .settings(Testing.settings: _*)
}
