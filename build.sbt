import BuildSettings._
import Dependencies._
import IntegrationTesting._
import com.typesafe.sbt.SbtMultiJvm._

lazy val root = (project in file("."))
  .configs( IntegrationTests )
  .configs( MultiJvm )
  .settings(
    basicSettings,
    integrationSettings,
    multiJvmSettings,
    jvmOptions in MultiJvm := Seq("-Xmx512M"),
    multiJvmMarker in MultiJvm := "ClusterTest",
    libraryDependencies ++=

      Dependencies.compile(scalaLogging, logbackClassic,
        akka, akkaCluster, akkaClusterSharding, akkaHttp, akkaStream, akkaHttpJson4s, akkaSlf4j,
         clusterMetricsExtension, json4sJackson, log4j_over_slf4j) ++

      Dependencies.test(scalaTest, json4sJackson, scalaMock, akkaHttpTestkit, akkaSlf4j, akkaHttpJson4s) ++

      Dependencies.integration(scalaTest, json4sJackson, scalaMock, akkaHttpTestkit, akkaHttpJson4s, scalaLogging,
        akkaSlf4j, log4j_over_slf4j) ++

      Dependencies.cluster(scalaTest, json4sJackson, scalaMock, akkaHttpTestkit, multiNodeTestkit, akkaHttpJson4s,
        scalaLogging, akkaSlf4j, log4j_over_slf4j)

  )