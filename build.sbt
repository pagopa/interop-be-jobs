import com.typesafe.sbt.packager.docker.Cmd

ThisBuild / scalaVersion      := "2.13.10"
ThisBuild / organization      := "it.pagopa"
ThisBuild / organizationName  := "Pagopa S.p.A."
ThisBuild / dependencyOverrides ++= Dependencies.Jars.overrides
ThisBuild / version           := ComputeVersion.version
Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / githubOwner                      := "pagopa"
ThisBuild / githubRepository                 := "interop-be-jobs"
ThisBuild / githubSuppressPublicationWarning := true
ThisBuild / resolvers += Resolver.githubPackages("pagopa")

lazy val attributesLoaderModuleName                  = "attributes-loader"
lazy val tokenDetailsPersisterModuleName             = "token-details-persister"
lazy val tenantsCertifiedAttributesUpdaterModuleName = "tenants-certified-attributes-updater"
lazy val metricsReportGeneratorModuleName            = "metrics-report-generator"
lazy val paDigitaleReportGeneratorModuleName         = "padigitale-report-generator"
lazy val dashboardMetricsGeneratorModuleName         = "dashboard-metrics-report-generator"
lazy val certifiedMailSenderModuleName               = "certified-mail-sender"
lazy val certifiedMailSenderModelsModuleName         = "certified-mail-sender-models"
lazy val eservicesFileGeneratorModuleName            = "eservices-file-generator"

cleanFiles += baseDirectory.value / certifiedMailSenderModuleName / "target"
cleanFiles += baseDirectory.value / certifiedMailSenderModelsModuleName / "target"

cleanFiles += baseDirectory.value / attributesLoaderModuleName / "target"
cleanFiles += baseDirectory.value / tokenDetailsPersisterModuleName / "target"

cleanFiles += baseDirectory.value / certifiedMailSenderModuleName / "target"
cleanFiles += baseDirectory.value / certifiedMailSenderModelsModuleName / "target"

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

lazy val paDigitaleReportGenerator = project
  .in(file(paDigitaleReportGeneratorModuleName))
  .settings(
    name                 := "interop-be-padigitale-report-generator",
    Docker / packageName := s"${name.value}",
    sharedSettings,
    libraryDependencies ++= Dependencies.Jars.paDigitaleReportGenerator,
    publish / skip       := true,
    publish              := (()),
    publishLocal         := (()),
    publishTo            := None
  )
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(DockerPlugin)

lazy val metricsReportGenerator = project
  .in(file(metricsReportGeneratorModuleName))
  .settings(
    name                 := "interop-be-metrics-report-generator",
    Docker / packageName := s"${name.value}",
    sharedSettings,
    libraryDependencies ++= Dependencies.Jars.metricsReportGenerator,
    publish / skip       := true,
    publish              := (()),
    publishLocal         := (()),
    publishTo            := None
  )
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(DockerPlugin)

lazy val dashboardMetricsGenerator = project
  .in(file(dashboardMetricsGeneratorModuleName))
  .settings(
    name                 := "interop-be-dashboard-metrics-report-generator",
    Docker / packageName := s"${name.value}",
    sharedSettings,
    libraryDependencies ++= Dependencies.Jars.dashboardMetricsReportGenerator,
    publish / skip       := true,
    publish              := (()),
    publishLocal         := (()),
    publishTo            := None
  )
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(DockerPlugin)

lazy val certifiedMailSender = project
  .in(file(certifiedMailSenderModuleName))
  .settings(
    name                 := "interop-be-certified-mail-sender",
    Docker / packageName := s"${name.value}",
    sharedSettings,
    libraryDependencies ++= Dependencies.Jars.certifiedMailSenderDependencies,
    publish / skip       := true,
    publish              := (()),
    publishLocal         := (()),
    publishTo            := None
  )
  .dependsOn(certifiedMailSenderModels)
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(DockerPlugin)

lazy val eservicesFileGenerator = project
  .in(file(eservicesFileGeneratorModuleName))
  .settings(
    name                 := "interop-be-eservices-file-generator",
    Docker / packageName := s"${name.value}",
    sharedSettings,
    libraryDependencies ++= Dependencies.Jars.eservicesFileGenerator,
    publish / skip       := true,
    publish              := (()),
    publishLocal         := (()),
    publishTo            := None
  )
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(DockerPlugin)

lazy val certifiedMailSenderModels = project
  .in(file(certifiedMailSenderModelsModuleName))
  .settings(name := "interop-be-certified-mail-sender-models", scalafmtOnCompile := true, Docker / publish := {})

lazy val jobs = project
  .in(file("."))
  .aggregate(
    paDigitaleReportGenerator,
    dashboardMetricsGenerator,
    tenantsCertifiedAttributesUpdater,
    tokenDetailsPersister,
    attributesLoader,
    metricsReportGenerator,
    certifiedMailSender,
    certifiedMailSenderModels,
    eservicesFileGenerator
  )
  .settings(Docker / publish := {})
