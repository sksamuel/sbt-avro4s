name := "sbt-avro4s"

organization := "com.sksamuel.avro4s"

version := "1.0.0"

scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8")

resolvers := ("releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2") +: resolvers.value

javacOptions ++= Seq("-source", "1.7", "-target", "1.7")

scalaVersion := "2.10.5"

sbtPlugin := true

publishMavenStyle := true

publishArtifact in Test := false

parallelExecution in Test := false
