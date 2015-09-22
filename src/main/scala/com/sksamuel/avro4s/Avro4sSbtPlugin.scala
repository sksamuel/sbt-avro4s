package com.sksamuel.avro4s

import java.nio.file.Paths

import sbt.Keys._
import sbt._

/** @author Stephen Samuel */
object Avro4sSbtPlugin extends AutoPlugin {

  val GroupId = "com.sksamuel.scapegoat"
  val ArtifactId = "scalac-scapegoat-plugin"

  object autoImport {
    lazy val avrogen = taskKey[Unit]("Generate case classes from avro files")
    lazy val avro4sOutputPath = settingKey[String]("where to write generated case classes")
    lazy val avro4sSourcePath = settingKey[String]("where to read avro schemas")
    lazy val avro4sSourceExt = settingKey[String]("The file extension to match for avro files")
  }

  import autoImport._

  override def trigger = allRequirements
  override def projectSettings = {
    Seq(
      avrogen := {
        streams.value.log.info(s"[sbt-avro4s] Generating sources from [${avro4sSourcePath.value}]")
        streams.value.log.info("--------------------------------------------------------------")

        val schemaFiles = new File(avro4sSourcePath.value).listFiles.filter(_.getName.endsWith(avro4sSourceExt.value))
        streams.value.log.info(s"[sbt-avro4s] Found ${schemaFiles.length} schemas")

        val defs = schemaFiles.flatMap(ClassGenerator.apply)
        streams.value.log.info(s"[sbt-avro4s] Generated ${defs.length} classes")

        FileRenderer.render(Paths.get(avro4sOutputPath.value), defs)
        streams.value.log.info(s"[sbt-avro4s] Wrote class files to [${avro4sOutputPath.value}]")
      }      ,
      avro4sSourcePath := "src/main/avro",
      avro4sOutputPath := "src/main/scala",
      avro4sSourceExt := "avsc"
    )
  }
}