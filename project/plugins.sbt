resolvers += Classpaths.sbtPluginReleases

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.1.0")

libraryDependencies += { "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value }

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.7")
