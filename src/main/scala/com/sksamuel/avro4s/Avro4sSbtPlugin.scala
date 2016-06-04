package com.sksamuel.avro4s

import sbt.Keys._
import sbt._

object Import {

  lazy val avro2Class = taskKey[Seq[File]]("Generate case classes from avro files")

  object Avro4sKeys {

  }

}

/** @author Stephen Samuel, Timo Merlin Zint */
object Avro4sSbtPlugin extends AutoPlugin {

  override def trigger = allRequirements

  val autoImport = Import

  import autoImport._
  import Avro4sKeys._

  override def projectSettings = Seq(
    includeFilter in avro2Class := "*.avsc",
    excludeFilter in avro2Class := HiddenFileFilter,
    resourceDirectory in avro2Class <<= (resourceDirectory in Compile) { _ / "avro" },
    sourceManaged in avro2Class <<= (sourceManaged in Compile) { _ / "avro" },
    managedSourceDirectories in Compile <+= sourceManaged in avro2Class,
    avro2Class := runAvro2Class.value,
    sourceGenerators in Compile += avro2Class.taskValue
  )

  private def runAvro2Class: Def.Initialize[Task[Seq[File]]] = Def.task {

    val inc = (includeFilter in avro2Class).value
    val exc = (excludeFilter in avro2Class).value || DirectoryFilter
    val inDir = (resourceDirectory in avro2Class).value
    val outDir = (sourceManaged in avro2Class).value

    streams.value.log.info(s"[sbt-avro4s] Generating sources from [${inDir}]")
    streams.value.log.info("--------------------------------------------------------------")

    val schemaFiles = Option(inDir.listFiles(inc -- exc)).filter(_.nonEmpty)
    streams.value.log.info(s"[sbt-avro4s] Found ${schemaFiles.fold(0)(_.length)} schemas")
    schemaFiles.map { f =>
      val defs = f.flatMap(ModuleGenerator.apply)
      streams.value.log.info(s"[sbt-avro4s] Generated ${defs.length} classes")

      val paths = FileRenderer.render(outDir.toPath, TemplateGenerator.apply(defs))
      streams.value.log.info(s"[sbt-avro4s] Wrote class files to [${outDir.toPath}]")

      paths
    }.getOrElse(Seq()).map(_.toFile)
  }

}
