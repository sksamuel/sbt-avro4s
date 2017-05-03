import sbt._
import Keys._

lazy val root = (project in file("."))
  .settings(
    name := "avro2Class",
    version := "0.1",
    scalaVersion := "2.10.5"
  ).enablePlugins(Avro4sSbtPlugin)
