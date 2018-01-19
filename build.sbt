import sbt._
import sbt.Keys._

val AvroVersion = "1.8.1"
val ScalatestVersion = "3.0.0"

lazy val commonSettings = Seq(
  scalaVersion := "2.12.4",
  organization := "com.simacan",
  scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8"),
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8")
)

lazy val root = (project in file(".")).
  enablePlugins(CommonSettings). // start with this
  settings(commonSettings: _*).
  settings(
    name := "sbt-avro4s",
    sbtPlugin := true,
    sbtVersion := "1.0.4",
    libraryDependencies ++= Seq(
      "org.apache.avro"       % "avro"                  % AvroVersion,
      "org.apache.avro"       % "avro-compiler"         % AvroVersion,
      "com.simacan"           %% "kafka-client-avro-resolver" % "0.0.1-SNAPSHOT",
      "org.scalatest"         %% "scalatest"            % ScalatestVersion % "test"
    ),
    publishArtifact in Test := false,
    parallelExecution in Test := false,
    scriptedLaunchOpts := { scriptedLaunchOpts.value ++ Seq(
      "-Xmx1024M",
      "-XX:MaxPermSize=256M",
      "-Dplugin.version=" + version.value
    )},
    scriptedBufferLog := false,

    resolvers := ("releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2") +: resolvers.value
  )
