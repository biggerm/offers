import sbt._

object Dependencies extends DependencyUtils {

  private object Version {
    lazy val config = "1.3.3"
    lazy val scalaLogging = "3.7.2"
    lazy val scalaTest = "3.0.3"
    lazy val scalaMock = "3.5.0"
    lazy val json4sJackson = "3.5.1"

    lazy val akka = "2.5.12"
    lazy val akkaHttp = "10.1.1"
    lazy val akkaHttpJson4s = "1.20.1"

    lazy val logback = "1.1.7"
    lazy val slf4j = "1.7.21"

  }

  // Akka centric (akka-http now has it's own project and version)
  val akka = "com.typesafe.akka" %% "akka-actor" % Version.akka
  val akkaRemote = "com.typesafe.akka" %% "akka-remote" % Version.akka
  val akkaCluster = "com.typesafe.akka" %% "akka-cluster" % Version.akka
  val akkaClusterSharding = "com.typesafe.akka" %% "akka-cluster-sharding" % Version.akka
  val akkaClusterTools = "com.typesafe.akka" %% "akka-cluster-tools" % Version.akka
  val akkaDistData = "com.typesafe.akka" %% "akka-distributed-data" % Version.akka
  val akkaSlf4j = "com.typesafe.akka" %% "akka-slf4j" % Version.akka
  val akkaStream = "com.typesafe.akka" %% "akka-stream" % Version.akka
  val akkaStreamTestkit = "com.typesafe.akka" %% "akka-stream-testkit" % Version.akka
  val clusterMetricsExtension = "com.typesafe.akka" %% "akka-cluster-metrics" % Version.akka
  val multiNodeTestkit = "com.typesafe.akka" %% "akka-multi-node-testkit" % Version.akka

  val akkaHttp = "com.typesafe.akka" %% "akka-http" % Version.akkaHttp
  val akkaHttpTestkit = "com.typesafe.akka" %% "akka-http-testkit" % Version.akkaHttp

  val config = "com.typesafe" % "config" % Version.config
  val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % Version.scalaLogging


  val json4sJackson = "org.json4s" %% "json4s-jackson" % Version.json4sJackson excludeAll ExclusionRule(organization = "org.scala-lang")
  val akkaHttpJson4s = "de.heikoseeberger" %% "akka-http-json4s" % Version.akkaHttpJson4s
  val logbackClassic = "ch.qos.logback" % "logback-classic" % Version.logback

  // Testing
  val scalaTest = "org.scalatest" %% "scalatest" % Version.scalaTest
  val scalaMock = "org.scalamock" %% "scalamock-scalatest-support" % Version.scalaMock

  val log4j_over_slf4j = "org.slf4j" % "log4j-over-slf4j" % Version.slf4j


}


