package com.sksamuel.avro4s

import sbt.Keys._
import sbt._
import plugins._

object Import {

  lazy val avro2Class = taskKey[Seq[File]]("Generate case classes from avro files; is a source sourceGenerator")

  object Avro4sKeys {
    val avroDirectoryName = SettingKey[String]("Recurrent directory name used for lookup and output")
    val avroFileEnding = SettingKey[String]("File ending of avro files, used for lookup and output")
  }

}

/** @author Stephen Samuel, Timo Merlin Zint */
object Avro4sSbtPlugin extends AutoPlugin {

  override def trigger = allRequirements
  override def requires = JvmPlugin // avoid override of sourceGenerators

  val autoImport = Import

  import autoImport._
  import Avro4sKeys._

  override def projectSettings = Seq(
    avroDirectoryName := "avro",
    avroFileEnding := "avsc",

    includeFilter in avro2Class := s"*.${avroFileEnding.value}",
    excludeFilter in avro2Class := HiddenFileFilter,
    resourceDirectory in avro2Class := (resourceDirectory in Compile).value / avroDirectoryName.value,
    sourceManaged in avro2Class := (sourceManaged in Compile).value / avroDirectoryName.value,
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

    val combinedFileFilter = inc -- exc
    val allFiles = getRecursiveListOfFiles(inDir)
    val schemaFiles = Option(allFiles.filter(combinedFileFilter.accept))
    streams.value.log.info(s"[sbt-avro4s] Found ${schemaFiles.fold(0)(_.length)} schemas")
    schemaFiles.map { f =>
      val defs = f.flatMap(ModuleGenerator.apply)
      streams.value.log.info(s"[sbt-avro4s] Generated ${defs.length} classes")

      val paths = FileRenderer.render(outDir.toPath, TemplateGenerator.apply(defs))
      streams.value.log.info(s"[sbt-avro4s] Wrote class files to [${outDir.toPath}]")

      paths
    }.getOrElse(Seq()).map(_.toFile)
  }

  def getRecursiveListOfFiles(dir: File): Array[File] = {
    val these = dir.listFiles
    if (these == null)
      Array.empty[File]
    else
      these ++ these.filter(_.isDirectory).flatMap(getRecursiveListOfFiles)
  }
}
