import com.typesafe.sbt.pgp.PgpKeys
import sbt._
import sbt.Keys._

val AvroVersion = "1.8.2"
val ScalatestVersion = "3.0.0"

lazy val commonSettings = Seq(
  organization := "com.sksamuel.avro4s",
  scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8"),
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8")
)

lazy val root = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "sbt-avro4s",
    sbtPlugin := true,
    licenses += ("MIT", url("https://opensource.org/licenses/MIT")),
    libraryDependencies ++= Seq(
      "org.apache.avro"       % "avro"                  % AvroVersion,
      "org.apache.avro"       % "avro-compiler"         % AvroVersion,
      "org.scalatest"         %% "scalatest"            % ScalatestVersion % "test"
    ),
    publishMavenStyle := true,
    SbtPgp.autoImport.useGpgAgent := true,
    SbtPgp.autoImport.useGpg := true,
    sbtrelease.ReleasePlugin.autoImport.releasePublishArtifactsAction := PgpKeys.publishSigned.value,
    sbtrelease.ReleasePlugin.autoImport.releaseCrossBuild := false,
    publishArtifact in Test := false,
    parallelExecution in Test := false,
    scriptedLaunchOpts := { scriptedLaunchOpts.value ++ Seq(
      "-Xmx1024M",
      "-XX:MaxPermSize=256M",
      "-Dplugin.version=" + version.value
    )},
    scriptedBufferLog := false,

    crossSbtVersions := Vector("0.13.16", "1.0.4"),

    resolvers := ("releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2") +: resolvers.value,

    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots") 
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },

    pomExtra := {
      <url>https://github.com/sksamuel/sbt-avro4s</url>
        <scm>
          <url>git@github.com:sksamuel/sbt-avro4s.git</url>
          <connection>scm:git@github.com:sksamuel/sbt-avro4s.git</connection>
        </scm>
        <developers>
          <developer>
            <id>sksamuel</id>
            <name>sksamuel</name>
            <url>http://github.com/sksamuel</url>
          </developer>
        </developers>
    }
  )
