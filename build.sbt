import ProjectSettings.ProjectFrom
import com.typesafe.sbt.packager.docker.Cmd

ThisBuild / scalaVersion := "2.13.6"
ThisBuild / organization := "it.pagopa"
ThisBuild / organizationName := "Pagopa S.p.A."
ThisBuild / libraryDependencies := Dependencies.Jars.cli.map(m =>
  if (scalaVersion.value.startsWith("3.0"))
    m.withDottyCompat(scalaVersion.value)
  else
    m
)

ThisBuild / dependencyOverrides ++= Dependencies.Jars.overrides
ThisBuild / version := ComputeVersion.version

ThisBuild / resolvers += "Pagopa Nexus Snapshots" at s"https://gateway.interop.pdnd.dev/nexus/repository/maven-snapshots/"
ThisBuild / resolvers += "Pagopa Nexus Releases" at s"https://gateway.interop.pdnd.dev/nexus/repository/maven-releases/"

val packagePrefix = settingKey[String]("The package prefix derived from the uservice name")

packagePrefix := name.value
  .replaceFirst("interop-", "interop.")
  .replaceFirst("be-", "")
  .replaceAll("-", "")

val projectName = settingKey[String]("The project name prefix derived from the uservice name")

projectName := name.value
  .replaceFirst("interop-", "")
  .replaceFirst("be-", "")

lazy val root = (project in file("."))
  .settings(
    name := "interop-be-attributes-loader",
    Test / parallelExecution := false,
    scalafmtOnCompile := true,
    dockerBuildOptions ++= Seq("--network=host"),
    dockerRepository := Some(System.getenv("DOCKER_REPO")),
    dockerBaseImage := "adoptopenjdk:11-jdk-hotspot",
    daemonUser := "daemon",
    Docker / version := s"${
      val buildVersion = (ThisBuild / version).value
      if (buildVersion == "latest")
        buildVersion
      else
        s"$buildVersion"
    }".toLowerCase,
    Docker / packageName := s"${name.value}",
//    Docker / dockerExposedPorts := Seq(8080),
    Docker / maintainer := "https://pagopa.it",
    dockerCommands += Cmd("LABEL", s"org.opencontainers.image.source https://github.com/pagopa/${name.value}")
  )
  .enablePlugins(JavaAppPackaging, JavaAgent)
  .setupBuildInfo

javaAgents += "io.kamon" % "kanela-agent" % "1.0.11"
Test / fork := true
Test / javaOptions += "-Dconfig.file=src/test/resources/application-test.conf"
