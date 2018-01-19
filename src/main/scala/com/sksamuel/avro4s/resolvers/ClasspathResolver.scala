package com.sksamuel.avro4s.resolvers

import java.nio.file.Files

import com.simacan.kafka.serialization.avro.resolver.SchemaSource
import com.simacan.kafka.serialization.avro.resolver.util.SchemaParser
import com.sun.nio.zipfs.ZipFileSystemProvider
import sbt.Keys.Classpath

class ClasspathResolver(classpath: Classpath) {
  
  def resolve(path: String): Option[SchemaSource] = {
    val files = classpath.filter(_.data.isFile).flatMap { jar =>
      import scala.collection.JavaConverters._

      val zip = new ZipFileSystemProvider().newFileSystem(jar.data.toPath, Map.empty[String, Int].asJava)

      try {
        val p = zip.getPath(path)
        if (Files.exists(p)) {
          val uri = p.toUri.toString
          val content = new String(Files.readAllBytes(p), "UTF-8")
          Some(SchemaSource(path, uri, content, SchemaParser.resolveIncludePaths(uri, content)))
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
}
