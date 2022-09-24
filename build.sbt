import com.typesafe.sbt.packager.docker.Cmd

ThisBuild / scalaVersion      := "2.13.9"
ThisBuild / organization      := "it.pagopa"
ThisBuild / organizationName  := "Pagopa S.p.A."
ThisBuild / dependencyOverrides ++= Dependencies.Jars.overrides
ThisBuild / version           := ComputeVersion.version
Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / resolvers += "Pagopa Nexus Snapshots" at s"https://${System.getenv("MAVEN_REPO")}/nexus/repository/maven-snapshots/"
ThisBuild / resolvers += "Pagopa Nexus Releases" at s"https://${System.getenv("MAVEN_REPO")}/nexus/repository/maven-releases/"

ThisBuild / publish / skip := true
ThisBuild / credentials += Credentials(Path.userHome / ".sbt" / ".credentials")

lazy val attributesLoaderModuleName                  = "attributes-loader"
lazy val tokenDetailsPersisterModuleName             = "token-details-persister"
lazy val tenantsCertifiedAttributesUpdaterModuleName = "tenants-certified-attributes-updater"

cleanFiles += baseDirectory.value / attributesLoaderModuleName / "target"
cleanFiles += baseDirectory.value / tokenDetailsPersisterModuleName / "target"

lazy val sharedSettings: SettingsDefinition = Seq(
  scalacOptions            := Seq(),
  scalafmtOnCompile        := true,
  updateOptions            := updateOptions.value.withGigahorse(false),
  Test / parallelExecution := false,
  dockerBuildOptions ++= Seq("--network=host"),
  dockerRepository         := Some(System.getenv("DOCKER_REPO")),
  dockerBaseImage          := "adoptopenjdk:11-jdk-hotspot",
  daemonUser               := "daemon",
  Docker / version         := (ThisBuild / version).value.replaceAll("-SNAPSHOT", "-latest").toLowerCase,
  Docker / maintainer      := "https://pagopa.it",
  dockerCommands += Cmd("LABEL", s"org.opencontainers.image.source https://github.com/pagopa/${name.value}")
)

lazy val attributesLoader = project
  .in(file(attributesLoaderModuleName))
  .settings(
    name                 := "interop-be-attributes-loader",
    Docker / packageName := s"${name.value}",
    sharedSettings,
    libraryDependencies ++= Dependencies.Jars.attributesLoader
  )
  .enablePlugins(JavaAppPackaging)

lazy val tokenDetailsPersister = project
  .in(file(tokenDetailsPersisterModuleName))
  .settings(
    name                 := "interop-be-token-details-persister",
    Docker / packageName := s"${name.value}",
    sharedSettings,
    libraryDependencies ++= Dependencies.Jars.tokenDetailsPersister
  )
  .enablePlugins(JavaAppPackaging)

lazy val tenantsCertifiedAttributesUpdater = project
  .in(file(tenantsCertifiedAttributesUpdaterModuleName))
  .settings(
    name                 := "interop-be-tenants-cert-attr-updater",
    Docker / packageName := s"${name.value}",
    sharedSettings,
    libraryDependencies ++= Dependencies.Jars.tenantsCertifiedAttributesUpdater
  )
  .enablePlugins(JavaAppPackaging)
