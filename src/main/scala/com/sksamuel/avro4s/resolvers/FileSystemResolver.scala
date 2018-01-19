package com.sksamuel.avro4s.resolvers

import java.nio.file.Files

import com.simacan.kafka.serialization.avro.resolver.SchemaSource
import com.simacan.kafka.serialization.avro.resolver.util.SchemaParser
import sbt.File

class FileSystemResolver(resourceDirs: Seq[File]) {

  def resolve(path: String): Option[SchemaSource] = {
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
        val uri = file.toURI.toString
        val content = new String(Files.readAllBytes(file.toPath), "UTF-8")
        SchemaSource(path, uri, content, SchemaParser.resolveIncludePaths(uri, content))
      }

    } else {
      throw new RuntimeException(s"Found more than one schema file for path $path at ${files.mkString(", ")}")
    }
  }
}
