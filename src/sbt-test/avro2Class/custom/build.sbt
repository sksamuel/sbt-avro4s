import sbt._
import Keys._
import Avro4sKeys._

lazy val root = (project in file("."))
  .enablePlugins(Avro4sSbtPlugin)
  .settings(
    name := "avro2Class",
    version := "0.1",
    scalaVersion := "2.12.4",
    Avro4sKeys.avroSchemaFiles in avro2Class := Seq("schemas/Pizza.avro")
  )
