package com.sksamuel.avro4s

import java.nio.file.{Files, Paths}
import java.util.Optional

import com.simacan.kafka.serialization.avro.resolver.{PathResolver, SchemaLoader, SchemaSource}
import com.sksamuel.avro4s.resolvers.{ClasspathResolver, FileSystemResolver}
import org.apache.avro.Protocol
import org.apache.avro.Schema.{Type => AvroType}
import sbt.Keys._
import sbt._
import sbt.plugins._

import scala.collection.JavaConverters._

object Import {

  lazy val avro2Class = taskKey[Seq[File]]("Generate case classes from avro schema files; is a source generator")
  lazy val avroIdl2Avro = taskKey[Seq[File]]("Generate avro schema files from avro IDL; is a resource generator")

  object Avro4sKeys {
    val avroSchemaFiles = SettingKey[Seq[String]]("avro-schema-files", "Avro schema files to compile")

    val avroDirectoryName = SettingKey[String]("avro-directory-name" , "Recurrent directory name used for lookup and output")
    val avroFileEnding = SettingKey[String]("avro-file-ending", "File ending of avro schema files, used for lookup and output")
    val avroIdlFileEnding = SettingKey[String]("avro-idl-file-ending", "File ending of avro IDL files, used for lookup and output")
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
    avroIdlFileEnding := "avdl",
    avroSchemaFiles in avro2Class := Seq.empty,

    resourceDirectory in avro2Class := (resourceDirectory in Compile).value,
    resourceDirectories in avro2Class := Seq(
      (resourceDirectory in avro2Class).value,
      (resourceManaged in avroIdl2Avro).value
    ),

    sourceManaged in avro2Class := (sourceManaged in Compile).value / avroDirectoryName.value,

    resourceDirectory in avroIdl2Avro := (resourceDirectory in Compile).value / avroDirectoryName.value,
    resourceManaged in avroIdl2Avro := (resourceManaged in Compile).value / avroDirectoryName.value,
    resources in avroIdl2Avro := getRecursiveListOfFiles((resourceDirectory in avroIdl2Avro).value),

    managedSourceDirectories in Compile += (sourceManaged in avro2Class).value,
    managedResourceDirectories in Compile += (resourceManaged in avroIdl2Avro).value,

    avro2Class := runAvro2Class.value,
    avroIdl2Avro := runAvroIdl2Avro.value,

    sourceGenerators in Compile += avro2Class.taskValue,
    resourceGenerators in Compile += avroIdl2Avro.taskValue
  )

  private def runAvro2Class: Def.Initialize[Task[Seq[File]]] = Def.task {

    val inDir = (resourceDirectories in avro2Class).value
    val outDir = (sourceManaged in avro2Class).value

    streams.value.log.info(s"[sbt-avro4s] Generating sources from [$inDir]")
    streams.value.log.info("--------------------------------------------------------------")

    val resourceDirs = (resourceDirectories in avro2Class).all(ScopeFilter(inAnyProject)).value.flatten

    val classpath = (dependencyClasspath in Compile).value

    val combinedResolver = new PathResolver {
      override def resolve(path: String): Optional[SchemaSource] = {
        new FileSystemResolver(resourceDirs).resolve(path).orElse(
          new ClasspathResolver(classpath).resolve(path)
        ).asJava
      }
    }

    val rootPaths = (avroSchemaFiles in avro2Class).value

    val schemaFiles = new SchemaLoader().loadAll(rootPaths, combinedResolver)

    streams.value.log.info(s"""[sbt-avro4s] Found ${schemaFiles.length} schemas at:\n - ${schemaFiles.map(s => s.path + " at " + s.uri).mkString("\n - ")}""")

    val defs = ModuleGenerator.fromSchemas(schemaFiles.map(_.content))
    streams.value.log.info(s"[sbt-avro4s] Generated ${defs.length} classes")

    val paths = FileRenderer.render(outDir.toPath, TemplateGenerator.apply(defs))
    streams.value.log.info(s"[sbt-avro4s] Wrote class files to [${outDir.toPath}]")

    paths.map(_.toFile)
  } dependsOn avroIdl2Avro

  private def runAvroIdl2Avro: Def.Initialize[Task[Seq[File]]] = Def.task {
    import org.apache.avro.compiler.idl.Idl

    val inc = (includeFilter in avroIdl2Avro).value
    val exc = (excludeFilter in avroIdl2Avro).value || DirectoryFilter
    val inDir = (resourceDirectory in avroIdl2Avro).value
    val outDir = (resourceManaged in avroIdl2Avro).value
    val outExt = s".${avroFileEnding.value}"

    streams.value.log.info(s"[sbt-avro4s] Generating sources from [$inDir]")
    streams.value.log.info("--------------------------------------------------------------")

    val combinedFileFilter = inc -- exc
    val allFiles = (resources in avroIdl2Avro).value
    val idlFiles = Option(allFiles.filter(combinedFileFilter.accept))
    streams.value.log.info(s"[sbt-avro4s] Found ${idlFiles.fold(0)(_.length)} IDLs")

    val schemata = idlFiles.map { f =>
      f.flatMap( file => {
        val idl = new Idl(file.getAbsoluteFile)
        val protocol: Protocol = idl.CompilationUnit()
        val protocolSchemata = protocol.getTypes
        idl.close()
        protocolSchemata.asScala
      }
      )
    }.getOrElse(Seq())

    val uniqueSchemata = schemata.groupBy(_.getFullName).mapValues { identicalSchemata =>
      val referenceSchema = identicalSchemata.head
      identicalSchemata.foreach { schema =>
        require(referenceSchema.equals(schema), s"Different schemata with name ${referenceSchema.getFullName} found")
      }
      referenceSchema
    }.values

    streams.value.log.info(s"[sbt-avro4s] Generated ${uniqueSchemata.size} unique schema(-ta)")

    Files.createDirectories(outDir.toPath)
    val schemaFiles = (for (s <- uniqueSchemata if s.getType == AvroType.RECORD) yield {
      val path = Paths.get(outDir.absolutePath, s.getFullName + outExt)
      val writer = Files.newBufferedWriter(path)
      writer.write(s.toString(true))
      writer.close()
      path.toFile
    }).toSeq

    streams.value.log.info(s"[sbt-avro4s] Wrote schema(-ta) to [${outDir.toPath}]")
    schemaFiles
  }

  def getRecursiveListOfFiles(dir: File): Array[File] = {
    val these = dir.listFiles
    if (these == null)
      Array.empty[File]
    else
      these ++ these.filter(_.isDirectory).flatMap(getRecursiveListOfFiles)
  }
}
