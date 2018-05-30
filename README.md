# sbt-avro4s

[<img src="https://img.shields.io/maven-central/v/com.sksamuel.avro4s/sbt-avro4s*.svg?label=latest%20release"/>](http://search.maven.org/#search%7Cga%7C1%7Csbt-avro4s)

## This project is now deprecated in favour of [sbt-avrohugger](https://github.com/julianpeeters/sbt-avrohugger). Continue to use [avro4s](https://github.com/sksamuel/avro4s) in conjection with avrohugger. No further bug fixes or features will be added to this library.

Sbt plugin to use avro4s to generate case classes from avro schemas

## Quickstart

* Add the plugin to your build, eg in `project/plugins.sbt` add this line:

```scala
 addSbtPlugin("com.sksamuel.avro4s" % "sbt-avro4s" % "1.0.2-SNAPSHOT")
```

## Version Compatibility

 sbt-avro4s | avro4s
 ---------- | -------
 1.0.0     | ≥1.6.1
 0.91.0     | 1.2.2

### Generate scala classes from Avro schema files

By default sbt-avro4s will look for `*.avsc` files in `src/main/resources/avro/`.
So put your schema files there and run:

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
avroDirectoryName                       | Recurrent directory name used for lookup and output
avroFileEnding                          | File ending of avro files, used for lookup and output
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
