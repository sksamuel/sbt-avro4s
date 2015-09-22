name := "sbt-avro4s"

organization := "com.sksamuel.avro4s"

version := "0.91.0"

scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8")

resolvers := ("releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2") +: resolvers.value

javacOptions ++= Seq("-source", "1.7", "-target", "1.7")

scalaVersion := "2.10.5"

libraryDependencies += "com.sksamuel.avro4s" %% "avro4s-generator" % "0.91.0"

sbtPlugin := true

publishMavenStyle := true

publishArtifact in Test := false

parallelExecution in Test := false

publishTo <<= version {
  (v: String) =>
    val nexus = "https://oss.sonatype.org/"
    if (v.trim.endsWith("SNAPSHOT"))
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

pomExtra := {
  <url>https://github.com/sksamuel/sbt-avro4s</url>
    <licenses>
      <license>
        <name>Apache 2</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
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