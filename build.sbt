import com.typesafe.sbt.packager.docker.Cmd

ThisBuild / scalaVersion := "2.13.6"
ThisBuild / organization := "it.pagopa"
ThisBuild / organizationName := "Pagopa S.p.A."
ThisBuild / dependencyOverrides ++= Dependencies.Jars.overrides
ThisBuild / version := ComputeVersion.version

ThisBuild / resolvers += "Pagopa Nexus Snapshots" at s"https://${System.getenv("MAVEN_REPO")}/nexus/repository/maven-snapshots/"
ThisBuild / resolvers += "Pagopa Nexus Releases" at s"https://${System.getenv("MAVEN_REPO")}/nexus/repository/maven-releases/"

ThisBuild / publish / skip := true
ThisBuild / credentials += Credentials(Path.userHome / ".sbt" / ".credentials")

lazy val attributesLoaderModuleName = "attributes-loader"

cleanFiles += baseDirectory.value / attributesLoaderModuleName / "target"

lazy val sharedSettings: SettingsDefinition = Seq(
  scalacOptions := Seq(),
  scalafmtOnCompile := true,
  updateOptions := updateOptions.value.withGigahorse(false),
  Test / parallelExecution := false,
  dockerBuildOptions ++= Seq("--network=host"),
  dockerRepository := Some(System.getenv("DOCKER_REPO")),
  dockerBaseImage := "adoptopenjdk:11-jdk-hotspot",
  daemonUser := "daemon",
  Docker / version := s"${
    val buildVersion = (ThisBuild / version).value
    if (buildVersion == "latest") buildVersion
    else s"$buildVersion"
  }".toLowerCase,
  Docker / maintainer := "https://pagopa.it",
  dockerCommands += Cmd("LABEL", s"org.opencontainers.image.source https://github.com/pagopa/${name.value}")
)

lazy val attributesLoader = project
  .in(file(attributesLoaderModuleName))
  .settings(
    name := "interop-be-attributes-loader",
    Docker / packageName := s"${name.value}",
    sharedSettings,
    libraryDependencies ++= Dependencies.Jars.attributesLoader
  )
  .enablePlugins(JavaAppPackaging, JavaAgent)
