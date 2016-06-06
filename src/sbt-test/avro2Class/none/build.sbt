import sbt._
import Keys._

lazy val root = (project in file("."))
  .settings(
    name := "avro2Class-none",
    version := "0.1",
    scalaVersion := "2.10.5"
  )
