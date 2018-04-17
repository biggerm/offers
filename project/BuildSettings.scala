import sbt.Keys._
import sbt._

object BuildSettings {

  lazy val basicSettings = Seq(
    version := "0.0.1",
    homepage := Some(new URL("https://github.com/example")),
    organization := "mrb",
    description := "Offers Project",
    scalaVersion := "2.12.5",
    parallelExecution in Test := false,
    scalacOptions := Seq(
      "-encoding", "utf8",
      "-feature",
      "-unchecked",
      "-deprecation",
      "-target:jvm-1.8",
      "-language:_",
      "-Xlog-reflective-calls",
      "-Ywarn-adapted-args"
    )
  )

}
