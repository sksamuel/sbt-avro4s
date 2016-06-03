# sbt-avro4s

Sbt plugin to use avro4s to generate case classes from avro schemas

## How to use

* Add the plugin to your build, eg in `project/plugins.sbt` add this line:

`addSbtPlugin("com.sksamuel.avro4s" % "sbt-avro4s" % "0.91.0")`

* Put your avro schemas in `src/main/resources/avro` and then run:

`sbt avrogen`

* The generated case classes will appear in the managedSourceDirectories
