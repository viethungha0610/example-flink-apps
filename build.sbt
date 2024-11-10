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

lazy val ProvidedRun = config("provided-run").extend(Compile)

inConfig(ProvidedRun)(Defaults.configSettings)

run in ProvidedRun := (run in Compile).evaluated
