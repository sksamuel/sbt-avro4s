# sbt-avro4s

Sbt plugin to use avro4s to generate case classes from avro schemas

## Quickstart

* Add the plugin to your build, eg in `project/plugins.sbt` add this line:

```scala
 addSbtPlugin("com.simacan.avro4s" % "sbt-avro4s" % "1.0.0")
```

## Version Compatibility

 sbt-avro4s | avro4s
 ---------- | -------
 1.0.0     | ≥1.6.1
 0.91.0     | 1.2.2

### Generate scala classes from Avro schema files

Generates code for all resolved schema's starting from the roots defined in avroSchemaFiles.

Schema's are resolved in the by checking the project resources directories first and 
and in the classpath second.

```scala
sbt avro2Class
```

or a depended task like:

```scala
sbt compile:managedSources
```


The case classes will get generated in `target/scala-2.10/src_managed/main/avro/`.

Also see `src/sbt-test/avro2Class` for examples.

## Settings

Option                                  | Description
----------------------------------------|----------------------------------------
avroDirectoryName                       | Directory name used for output
avroFileEnding                          | File ending of avro files, used for output
resourceDirectory in avro2Class         | Input directory for the avro2Class task
sourceManaged in avro2Class             | Output directory for the avro2Class task

## Tasks

Task                                    | Description
----------------------------------------|----------------------------------------
avro2Class                              | Generate case classes from avro files; is a source sourceGenerator

## Development

### Testing

To run all the sbt plugin test:
```scala
sbt scripted
```

To run the `B` test of the test group `A`:
```scala
sbt scripted A/B
```

e.g. :
```scala
sbt scripted avro2Class/simple
```
