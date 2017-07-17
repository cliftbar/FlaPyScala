name := """FlaPyScala"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.11"
fork in run := false
PlayKeys.fileWatchService := play.runsupport.FileWatchService.sbt(pollInterval.value)

libraryDependencies += jdbc
libraryDependencies += cache
libraryDependencies += ws
libraryDependencies += filters
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0" % Test

test in assembly := {}

// Assmebly
// https://www.playframework.com/documentation/2.3.9/ProductionDist
mainClass in assembly := Some("play.core.server.NettyServer")

fullClasspath in assembly += Attributed.blank(PlayKeys.playPackageAssets.value)

// Exclude commons-logging because it conflicts with the jcl-over-slf4j
libraryDependencies ~= { _ map {
  case m if m.organization == "com.typesafe.play" =>
    m.exclude("commons-logging", "commons-logging")
  case m => m
}}

// Take the first ServerWithStop because it's packaged into two jars
assemblyMergeStrategy in assembly := {
  case PathList("play", "core", "server", "ServerWithStop.class") => MergeStrategy.first
  // Firx netty error with io.netty.versions.properties
  // https://github.com/outworkers/phantom/issues/405
  case x if x.endsWith("io.netty.versions.properties") => MergeStrategy.first
  case other => (assemblyMergeStrategy in assembly).value(other)
}