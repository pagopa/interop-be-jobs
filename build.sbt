import com.typesafe.sbt.packager.docker.Cmd

ThisBuild / scalaVersion      := "2.13.10"
ThisBuild / organization      := "it.pagopa"
ThisBuild / organizationName  := "Pagopa S.p.A."
ThisBuild / dependencyOverrides ++= Dependencies.Jars.overrides
ThisBuild / version           := ComputeVersion.version
Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / githubSuppressPublicationWarning := true
ThisBuild / resolvers += Resolver.githubPackages("pagopa")

lazy val attributesLoaderModuleName                  = "attributes-loader"
lazy val tokenDetailsPersisterModuleName             = "token-details-persister"
lazy val tenantsCertifiedAttributesUpdaterModuleName = "tenants-certified-attributes-updater"

cleanFiles += baseDirectory.value / attributesLoaderModuleName / "target"
cleanFiles += baseDirectory.value / tokenDetailsPersisterModuleName / "target"

lazy val sharedSettings: SettingsDefinition = Seq(
  scalafmtOnCompile        := true,
  updateOptions            := updateOptions.value.withGigahorse(false),
  Test / parallelExecution := false,
  dockerBuildOptions ++= Seq("--network=host"),
  dockerRepository         := Some(System.getenv("ECR_REGISTRY")),
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
    libraryDependencies ++= Dependencies.Jars.attributesLoader,
    publish / skip       := true,
    publish              := (()),
    publishLocal         := (()),
    publishTo            := None
  )
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(DockerPlugin)

lazy val tokenDetailsPersister = project
  .in(file(tokenDetailsPersisterModuleName))
  .settings(
    name                 := "interop-be-token-details-persister",
    Docker / packageName := s"${name.value}",
    sharedSettings,
    libraryDependencies ++= Dependencies.Jars.tokenDetailsPersister,
    publish / skip       := true,
    publish              := (()),
    publishLocal         := (()),
    publishTo            := None
  )
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(DockerPlugin)

lazy val tenantsCertifiedAttributesUpdater = project
  .in(file(tenantsCertifiedAttributesUpdaterModuleName))
  .settings(
    name                 := "interop-be-tenants-cert-attr-updater",
    Docker / packageName := s"${name.value}",
    sharedSettings,
    libraryDependencies ++= Dependencies.Jars.tenantsCertifiedAttributesUpdater,
    publish / skip       := true,
    publish              := (()),
    publishLocal         := (()),
    publishTo            := None
  )
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(DockerPlugin)

lazy val jobs = project
  .in(file("."))
  .aggregate(tenantsCertifiedAttributesUpdater, tokenDetailsPersister, attributesLoader)
  .settings(Docker / publish := {})
