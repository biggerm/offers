import sbt._

object IntegrationTesting {
  lazy val IntegrationTests = config("it") extend(Test)
  lazy val integrationSettings = Seq(inConfig(IntegrationTests)(Defaults.testSettings) : _*)
}