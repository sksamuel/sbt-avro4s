resolvers += Classpaths.sbtPluginReleases

addSbtPlugin("com.simacan" % "services-parent" % "0.6.0-SNAPSHOT")

libraryDependencies += {
  "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
}