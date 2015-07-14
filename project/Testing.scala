import sbt._
import sbt.Keys._

object Testing {
  import Configs._

  lazy val testAll = TaskKey[Unit]("test-all")

  private lazy val itSettings =
    inConfig(IntegrationTest)(Defaults.testSettings) ++
    Seq(
      fork in IntegrationTest := false,
      parallelExecution in IntegrationTest := false,
      scalaSource in IntegrationTest := baseDirectory.value / "src/it/scala")

  private lazy val e2eSettings =
    inConfig(EndToEndTest)(Defaults.testSettings) ++
    Seq(
      fork in EndToEndTest := false,
      parallelExecution in EndToEndTest := false,
      scalaSource in EndToEndTest := baseDirectory.value / "src/e2e/scala")

  lazy val settings = itSettings ++ e2eSettings ++ Seq(
    testAll <<= (test in EndToEndTest).dependsOn((test in IntegrationTest).dependsOn(test in Test))
  )
}
