package com.sksamuel.avro4s

import java.nio.file.{Files, Paths}

import com.sun.nio.zipfs.ZipFileSystemProvider
import org.apache.avro.Protocol
import org.apache.avro.Schema.{Type => AvroType}
import play.api.libs.json.{JsArray, JsNull, JsString, Json}
import sbt.Keys._
import sbt._
import sbt.plugins._

import scala.collection.JavaConverters._

object Import {

  lazy val avro2Class = taskKey[Seq[File]]("Generate case classes from avro schema files; is a source generator")
  lazy val avroIdl2Avro = taskKey[Seq[File]]("Generate avro schema files from avro IDL; is a resource generator")

  object Avro4sKeys {
    val avroSchemaFiles = SettingKey[Seq[String]]("Avro schema files to compile")

    val avroDirectoryName = SettingKey[String]("Recurrent directory name used for lookup and output")
    val avroFileEnding = SettingKey[String]("File ending of avro schema files, used for lookup and output")
    val avroIdlFileEnding = SettingKey[String]("File ending of avro IDL files, used for lookup and output")
  }

}

/** @author Stephen Samuel, Timo Merlin Zint */
object Avro4sSbtPlugin extends AutoPlugin {

  override def trigger = allRequirements
  override def requires = JvmPlugin // avoid override of sourceGenerators

  val autoImport = Import

  import autoImport._
  import Avro4sKeys._

  case class SchemaSource(
    path: String,
    uri: URI,
    content: String
  )

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

    managedSourceDirectories in Compile <+= sourceManaged in avro2Class,
    managedResourceDirectories in Compile <+= resourceManaged in avroIdl2Avro,

    avro2Class := runAvro2Class.value,
    avroIdl2Avro := runAvroIdl2Avro.value,

    sourceGenerators in Compile += avro2Class.taskValue,
    resourceGenerators in Compile += avroIdl2Avro.taskValue
  )

  private def runAvro2Class: Def.Initialize[Task[Seq[File]]] = Def.task {

    val inDir = (resourceDirectories in avro2Class).value
    val outDir = (sourceManaged in avro2Class).value

    streams.value.log.info(s"[sbt-avro4s] Generating sources from [${inDir}]")
    streams.value.log.info("--------------------------------------------------------------")

    val resourceDirs = (resourceDirectories in avro2Class).all(ScopeFilter(inAnyProject)).value.flatten

    val classpath = (dependencyClasspath in Compile).value

    val schemaFiles = (avroSchemaFiles in avro2Class).value.flatMap(path => loadSchemaFiles(path, resourceDirs, classpath))

    streams.value.log.info(s"""[sbt-avro4s] Found ${schemaFiles.length} schemas at:\n - ${schemaFiles.map(s => s.path + " at " + s.uri).mkString("\n - ")}""")

    val defs = ModuleGenerator.fromSchemas(schemaFiles.map(_.content))
    streams.value.log.info(s"[sbt-avro4s] Generated ${defs.length} classes")

    val paths = FileRenderer.render(outDir.toPath, TemplateGenerator.apply(defs))
    streams.value.log.info(s"[sbt-avro4s] Wrote class files to [${outDir.toPath}]")

    paths.map(_.toFile)
  } dependsOn avroIdl2Avro

  private def loadSchemaFiles(path: String, resourceDirs: Seq[File], classPath: Classpath): Seq[SchemaSource] = {
    resolveInFileSystem(path, resourceDirs).orElse(resolveInClasspath(path, classPath)).map {
      case schemaSource @ SchemaSource(_, uri, content) =>
        val json = Json.parse(content)

        val includes = (json \\ "include").flatMap {
          case JsString(include) => Seq(include)
          case JsArray(values) => values.collect {
            case JsString(include) => include
          }
          case JsNull => Seq.empty
          case other => throw new RuntimeException(s"Illegal include value $other. Expected a string, an array of strings or null.")
        }

        includes.flatMap(path => loadSchemaFiles(path, resourceDirs, classPath)) :+ schemaSource
    }.getOrElse(throw new RuntimeException(s"Schema $path not found"))
  }

  private def resolveInFileSystem(path: String, resourceDirs: Seq[File]): Option[SchemaSource] = {
    //val files = (resourceDirectories in Compile).all(ScopeFilter(inAnyProject)).value.flatMap { dirs =>
    val files = resourceDirs.flatMap { dir =>
      val file = dir.toPath.resolve(new File(path).toPath).toFile
      if (file.exists()) {
        Some(file)
      } else {
        None
      }
    }.distinct

    if (files.isEmpty) {
      None
    } else if (files.lengthCompare(1) == 0) {
      files.headOption.map { file =>
        val content = new String(Files.readAllBytes(file.toPath), "UTF-8")
        SchemaSource(path, file.toURI, content)
      }

    } else {
      throw new RuntimeException(s"Found more than one schema file for path $path at ${files.mkString(", ")}")
    }
  }

  private def resolveInClasspath(path: String, classpath: Classpath): Option[SchemaSource] = {
    //val files = (dependencyClasspath in Compile).value.filter(_.data.isFile).flatMap { jar =>
    val files = classpath.filter(_.data.isFile).flatMap { jar =>
      import scala.collection.JavaConverters._

      val zip = new ZipFileSystemProvider().newFileSystem(jar.data.toPath, Map.empty[String, Int].asJava)

      try {
        val p = zip.getPath(path)
        if (Files.exists(p)) {
          val content = new String(Files.readAllBytes(p), "UTF-8")
          Some(SchemaSource(path, p.toUri, content))
        } else {
          None
        }
      } finally {
        zip.close()
      }
    }
    if (files.isEmpty) {
      None
    } else if (files.lengthCompare(1) == 0) {
      files.headOption
    } else {
      throw new RuntimeException(s"Found more than one schema file for path $path at ${files.mkString(", ")}")
    }
  }

  private def runAvroIdl2Avro: Def.Initialize[Task[Seq[File]]] = Def.task {
    import org.apache.avro.compiler.idl.Idl

    val inc = (includeFilter in avroIdl2Avro).value
    val exc = (excludeFilter in avroIdl2Avro).value || DirectoryFilter
    val inDir = (resourceDirectory in avroIdl2Avro).value
    val outDir = (resourceManaged in avroIdl2Avro).value
    val outExt = s".${avroFileEnding.value}"

    streams.value.log.info(s"[sbt-avro4s] Generating sources from [${inDir}]")
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
      ).toSeq
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
