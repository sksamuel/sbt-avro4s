package com.sksamuel.avro4s

class ModuleRenderer {

  def apply(record: RecordType): String = {
    val caseClass = s"//auto generated code by avro4s\ncase class ${record.name}(\n" + record.fields.map(TypeRenderer.render).mkString(",\n") + "\n)"

    val unionFieldTypes = record.fields.collect {
      case FieldDef(name, unionType@UnionType(types)) if types.count(_ != NullType) > 2 =>
        s"  type ${name.capitalize}Type = ${TypeRenderer.renderType(unionType)}"
    }

    if (unionFieldTypes.nonEmpty) {
      val companionObject = s"//auto generated code by avro4s\nobject ${record.name} {\n" +
        unionFieldTypes.mkString("\n") +
        "\n}"
      caseClass ++ "\n\n" ++ companionObject
    } else {
      caseClass
    }
  }

  def apply(fixed: FixedType): String = {
    s"//auto generated code by avro4s\n@com.sksamuel.avro4s.AvroFixed(${fixed.length})\ncase class ${fixed.name}(bytes: scala.collection.mutable.WrappedArray.ofByte) extends AnyVal"
  }

  def apply(enum: EnumType): String = {
    s"//auto generated code by avro4s\npublic enum ${enum.name}" + enum.symbols.mkString("{\n    ", ", ", "\n}")
  }

  def apply(module: Module): String = {
    module match {
      case r: RecordType => apply(r)
      case e: EnumType => apply(e)
      case f: FixedType => apply(f)
    }
  }
}
