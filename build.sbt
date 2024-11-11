import Dependencies.deps

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.15"
ThisBuild / organization := "com.viethungha"
ThisBuild / organizationName := "viethungha"

lazy val root = (project in file(".")).settings(
  name := "example-flink-apps",
  libraryDependencies ++= deps
)

resolvers ++= (
  Resolver.sonatypeOssRepos("releases") ++ Seq("Confluent".at("https://packages.confluent.io/maven/"))
)

mainClass in (Compile, run) := Some("com.viethungha.flink.examples.PageviewAgg")
mainClass in (Compile, packageBin) := Some(
  "com.viethungha.flink.examples.PageviewAgg"
)

assembly / assemblyMergeStrategy := {
  case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
  case PathList("META-INF", xs @ _*)       => MergeStrategy.discard
  case _                                   => MergeStrategy.first
}
