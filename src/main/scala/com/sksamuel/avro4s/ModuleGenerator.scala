package com.sksamuel.avro4s

import java.io.{File, InputStream}
import java.nio.file.{Files, Path, Paths}

import org.apache.avro.Schema
import org.apache.avro.Schema.Parser

import scala.reflect.internal.SymbolTable

object ModuleGenerator {

  import scala.collection.JavaConverters._

  def apply(in: InputStream): Seq[Module] = ModuleGenerator(Seq(new Parser().parse(in)))
  def apply(file: File): Seq[Module] = ModuleGenerator.fromFiles(Seq(file))

  def fromFiles(files: Seq[File]): Seq[Module] = {
    val parser = new Parser()
    ModuleGenerator {
      files.sortBy(_.getName).map(parser.parse)
    }
  }

  def fromSchemas(schemas: Seq[String]): Seq[Module] = {
    val parser = new Parser()
    ModuleGenerator {
      schemas.map(parser.parse)
    }
  }


  def apply(schemata: Seq[Schema]): Seq[Module] = {

    val types = scala.collection.mutable.Map.empty[String, Module]

    def schemaToType(schema: Schema): Type = {
      schema.getType match {
        case Schema.Type.ARRAY => ArrayType(schemaToType(schema.getElementType))
        case Schema.Type.BOOLEAN => PrimitiveType.Boolean
        case Schema.Type.BYTES if schema.getProp("logicalType") == "decimal" => PrimitiveType.BigDecimal
        case Schema.Type.BYTES => PrimitiveType.Bytes
        case Schema.Type.DOUBLE => PrimitiveType.Double
        case Schema.Type.ENUM => types.getOrElse(schema.getFullName, enumFor(schema))
        case Schema.Type.FIXED => types.getOrElse(schema.getFullName, fixedFor(schema))
        case Schema.Type.FLOAT => PrimitiveType.Float
        case Schema.Type.INT => PrimitiveType.Int
        case Schema.Type.LONG => PrimitiveType.Long
        case Schema.Type.MAP => MapType(schemaToType(schema.getValueType))
        case Schema.Type.NULL => NullType
        case Schema.Type.RECORD => types.getOrElse(schema.getFullName, recordFor(schema))
        case Schema.Type.STRING => PrimitiveType("String")
        case Schema.Type.UNION => UnionType(schema.getTypes.asScala.toSeq.map(schemaToType _))
        case _ => sys.error("Unsupported field type: " + schema.getType)
      }
    }

    def enumFor(schema: Schema): EnumType = {
      val enum = EnumType(schema.getNamespace, schema.getName, schema.getEnumSymbols.asScala.toList)
      types.put(schema.getFullName, enum)
      enum
    }

    def fixedFor(schema: Schema): FixedType = {
      val fixed = FixedType(schema.getNamespace, schema.getName, schema.getFixedSize)
      types.put(schema.getFullName, fixed)
      fixed
    }

    def recordFor(schema: Schema): RecordType = {
      val record = RecordType(schema.getNamespace, schema.getName, Nil)
      types.put(schema.getFullName, record)
      val updated = record.copy(fields = schema.getFields.asScala.map { field =>
        FieldDef(field.name, schemaToType(field.schema))
      })
      types.put(schema.getFullName, updated)
      updated
    }

//    schemata.foreach { schema =>
//      require(schema.getType == Schema.Type.RECORD)
//    }

    val flattened = schemata.flatMap(flattenSchema)

    flattened.foreach(recordFor)
    types.values.toList
  }

  /**
    * A schema can be a union of schemas, but the ModuleGenerator does not support this, so we flatten the UNIONs first.
    */
  private[this] def flattenSchema(schema: Schema) = {
    schema.getType match {
      case Schema.Type.UNION => schema.getTypes.asScala.toList
      case _ => List(schema)
    }
  }
}

sealed trait Type

// a module is a case class (avro record) or java enum (avro enum); always needs a name + package (namespace)
sealed trait Module extends Type {
  def namespace: String
  def name: String
}

case class RecordType(namespace: String, name: String, fields: Seq[FieldDef]) extends Module

case class EnumType(namespace: String, name: String, symbols: Seq[String]) extends Module

case class MapType(valueType: Type) extends Type

case class FixedType(namespace: String, name: String, length: Int) extends Module

case class PrimitiveType(baseType: String) extends Type

object PrimitiveType {
  val Bytes = PrimitiveType("Array[Byte]")
  val BigDecimal = PrimitiveType("BigDecimal")
  val Float = PrimitiveType("Float")
  val Double = PrimitiveType("Double")
  val Long = PrimitiveType("Long")
  val String = PrimitiveType("String")
  val Int = PrimitiveType("Int")
  val Boolean = PrimitiveType("Boolean")
}

case class ArrayType(arrayType: Type) extends Type

case class UnionType(types: Seq[Type]) extends Type
object UnionType {
  def apply(ts: Type*)(implicit dummy: DummyImplicit): UnionType = UnionType(ts)
}

case object NullType extends Type

case class FieldDef(name: String, `type`: Type) {
  lazy val escapedName = {
    if (FieldDef.isReservedKeyword(name)) {
      s"`$name`"
    } else {
      name
    }
  }
}

private object FieldDef {
  val universe = scala.reflect.runtime.universe
  val symbols = universe.asInstanceOf[SymbolTable]

  def isReservedKeyword(name: String): Boolean = {
    symbols.nme.keywords.contains(symbols.newTermNameCached(name))
  }
}

object TypeRenderer {
  def render(f: FieldDef): String = s"  ${f.escapedName}: ${renderType(f.`type`)}"
  def renderType(t: Type, forceCoproduct: Boolean = false): String = {
    t match {
      case PrimitiveType(base) => base
      case ArrayType(arrayType) => s"Seq[${renderType(arrayType)}]"
      case RecordType(namespace, name, _) => namespace + "." + name
      case EnumType(namespace, name, _) => namespace + "." + name
      case MapType(valueType) => s"Map[String, ${renderType(valueType)}]"
      case FixedType(namespace, name, _) => namespace + "." + name
      case UnionType(Seq()) => "shapeless.CNil"
      case UnionType(Seq(NullType, right)) => s"Option[${renderType(right)}]"
      case UnionType(Seq(left, right)) if !forceCoproduct => s"Either[${renderType(left)}, ${renderType(right)}]"
      case UnionType(NullType +: seq) => s"Option[${renderType(UnionType(seq))}]"
      case UnionType(head +: tail) => s"shapeless.:+:[${renderType(head)}, ${renderType(UnionType(tail), forceCoproduct = true)}]"
      case NullType => "null"
    }
  }
}


// templates contains all generated definitions grouped by file
case class Template(file: String, extension: String, definition: String)


object FileRenderer {
  def output(dir: Path, template: Seq[Template]): Map[Path, String] = template.map { template =>
    val targetPath = dir resolve Paths.get(template.file.replace(".", File.separator) + s".${template.extension}")
    targetPath -> template.definition
  }.toMap

  def render(dir: Path, template: Seq[Template]): Seq[Path] = {
    output(dir, template).map {
      case (targetPath, contents) =>
        val packagePath = targetPath.getParent
        packagePath.toFile.mkdirs()

        val writer = Files.newBufferedWriter(targetPath)
        writer.write(contents)
        writer.close()
        targetPath
    }.toSeq
  }
}
