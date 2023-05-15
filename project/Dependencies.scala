import PagopaVersions._
import Versions._
import sbt._

object Dependencies {

  private[this] object akka {
    lazy val namespace     = "com.typesafe.akka"
    lazy val actorTyped    = namespace %% "akka-actor-typed"           % akkaVersion
    lazy val actor         = namespace %% "akka-actor"                 % akkaVersion
    lazy val serialization = namespace %% "akka-serialization-jackson" % akkaVersion
    lazy val stream        = namespace %% "akka-stream"                % akkaVersion
    lazy val slf4j         = namespace %% "akka-slf4j"                 % akkaVersion

  }

  private[this] object cats {
    lazy val namespace = "org.typelevel"
    lazy val core      = namespace %% "cats-core" % catsVersion
  }

  private[this] object circe {
    lazy val namespace = "io.circe"
    lazy val core      = namespace %% "circe-core"    % circeVersion
    lazy val generic   = namespace %% "circe-generic" % circeVersion
    lazy val parser    = namespace %% "circe-parser"  % circeVersion

  }

  private[this] object jackson {
    lazy val namespace   = "com.fasterxml.jackson.core"
    lazy val core        = namespace % "jackson-core"         % jacksonVersion
    lazy val annotations = namespace % "jackson-annotations"  % jacksonVersion
    lazy val databind    = namespace % "jackson-databind"     % jacksonVersion
    lazy val scalaModule = namespace % "jackson-module-scala" % jacksonVersion
  }

  private[this] object logback {
    lazy val namespace = "ch.qos.logback"
    lazy val classic   = namespace % "logback-classic" % logbackVersion
  }

  private[this] object mongodb {
    lazy val scalaDriver = "org.mongodb.scala" %% "mongo-scala-driver" % mongodbScalaDriverVersion
  }

  private[this] object pagopa {
    lazy val namespace = "it.pagopa"

    lazy val attributeRegistryProcess =
      namespace %% "interop-be-attribute-registry-process-client" % attributeRegistryProcessVersion

    lazy val attributeModels =
      namespace %% "interop-be-attribute-registry-management-models" % attributeRegistryManagementVersion

    lazy val catalogModels =
      namespace %% "interop-be-catalog-management-models" % catalogManagementVersion

    lazy val partyRegistryProxy =
      namespace %% "interop-be-party-registry-proxy-client" % partyRegistryProxyVersion

    lazy val tenantModels =
      namespace %% "interop-be-tenant-management-models" % tenantManagementVersion

    lazy val tenantManagement =
      namespace %% "interop-be-tenant-management-client" % tenantManagementVersion
  
    lazy val tenantProcess =
      namespace %% "interop-be-tenant-process-client" % tenantProcessVersion

    lazy val agreementsModels =
      namespace %% "interop-be-agreement-management-models" % agreementManagementVersion

    lazy val purposeModels =
      namespace %% "interop-be-purpose-management-models" % purposeManagementVersion

    lazy val partyManagement =
      namespace %% "interop-selfcare-party-management-client" % partyManagementClientVersion

    lazy val commons = namespace %% "interop-commons-utils"         % commonsVersion
    lazy val mail    = namespace %% "interop-commons-mail-manager"  % commonsVersion
    lazy val jwt     = namespace %% "interop-commons-jwt"           % commonsVersion
    lazy val signer  = namespace %% "interop-commons-signer"        % commonsVersion
    lazy val queue   = namespace %% "interop-commons-queue-manager" % commonsVersion
    lazy val cqrs    = namespace %% "interop-commons-cqrs"          % commonsVersion
    lazy val file    = namespace %% "interop-commons-file-manager"  % commonsVersion
    lazy val parser  = namespace %% "interop-commons-parser"        % commonsVersion
  }

  private[this] object scanamo {
    lazy val scanamo = "org.scanamo" %% "scanamo" % scanamoVersion
    lazy val testkit = "org.scanamo" %% "scanamo-testkit" % scanamoVersion
  }

  private[this] object sttp {
    lazy val sttpClient = "com.softwaremill.sttp.client4" %% "core" % sttpVersion
    lazy val sttpModel  = "com.softwaremill.sttp.model" %% "core" % sttpModelVersion
    lazy val stpCirce   = "com.softwaremill.sttp.client4" %% "circe" % sttpVersion
    lazy val stpLog     = "com.softwaremill.sttp.client4" %% "slf4j-backend" % sttpVersion
  }

  private[this] object scalameta {
    lazy val namespace = "org.scalameta"
    lazy val munit     = namespace %% "munit" % munitVersion
  }

  object Jars {
    lazy val overrides: Seq[ModuleID] = Seq(
      jackson.annotations % Compile,
      jackson.core        % Compile,
      jackson.databind    % Compile,
      jackson.scalaModule % Compile,
      pagopa.commons      % Compile
    ).map(_.withSources.withJavadoc)

    lazy val attributesLoader: Seq[ModuleID] = Seq(
      // For making Java 12 happy
      "javax.annotation"              % "javax.annotation-api" % "1.3.2" % "compile",
      //
      akka.actor                      % Compile,
      akka.actorTyped                 % Compile,
      akka.serialization              % Compile,
      akka.slf4j                      % Compile,
      akka.stream                     % Compile,
      logback.classic                 % Compile,
      pagopa.attributeRegistryProcess % Compile,
      pagopa.commons                  % Compile,
      pagopa.jwt                      % Compile,
      pagopa.signer                   % Compile
    ).map(_.withSources.withJavadoc)

    lazy val tokenDetailsPersister: Seq[ModuleID] = Seq(
      // For making Java 12 happy
      "javax.annotation" % "javax.annotation-api" % "1.3.2" % "compile",
      logback.classic    % Compile,
      pagopa.file        % Compile,
      pagopa.queue       % Compile
    ).map(_.withSources.withJavadoc)

    lazy val tenantsCertifiedAttributesUpdater: Seq[ModuleID] = Seq(
      // For making Java 12 happy
      "javax.annotation"        % "javax.annotation-api" % "1.3.2" % "compile",
      akka.actorTyped           % Compile,
      cats.core                 % Compile,
      logback.classic           % Compile,
      mongodb.scalaDriver       % Compile,
      pagopa.attributeModels    % Compile,
      pagopa.partyRegistryProxy % Compile,
      pagopa.tenantModels       % Compile,
      pagopa.tenantProcess      % Compile,
      pagopa.jwt                % Compile,
      pagopa.signer             % Compile,
      scalameta.munit           % Test
    ).map(_.withSources.withJavadoc)

    lazy val metricsReportGenerator: Seq[ModuleID] = Seq(
      // For making Java 12 happy
      "javax.annotation"       % "javax.annotation-api" % "1.3.2" % "compile",
      cats.core                % Compile,
      "com.github.pureconfig" %% "pureconfig"           % "0.17.2",
      circe.core               % Compile,
      circe.generic            % Compile,
      logback.classic          % Compile,
      mongodb.scalaDriver      % Compile,
      pagopa.catalogModels     % Compile,
      pagopa.tenantModels      % Compile,
      pagopa.agreementsModels  % Compile,
      pagopa.purposeModels     % Compile,
      pagopa.commons           % Compile,
      pagopa.mail              % Compile,
      pagopa.cqrs              % Compile,
      pagopa.file              % Compile,
      pagopa.parser            % Compile
    ).map(_.withSources.withJavadoc)

    lazy val dashboardMetricsReportGenerator: Seq[ModuleID] = Seq(
      cats.core                % Compile,
      "com.github.pureconfig" %% "pureconfig" % "0.17.2",
      logback.classic          % Compile,
      mongodb.scalaDriver      % Compile,
      pagopa.catalogModels     % Compile,
      pagopa.tenantModels      % Compile,
      pagopa.agreementsModels  % Compile,
      pagopa.partyManagement   % Compile,
      pagopa.purposeModels     % Compile,
      pagopa.commons           % Compile,
      pagopa.cqrs              % Compile,
      pagopa.file              % Compile
    ).map(_.withSources.withJavadoc)

    val paDigitaleReportGenerator: Seq[ModuleID] = Seq(
      cats.core            % Compile,
      circe.core           % Compile,
      circe.generic        % Compile,
      logback.classic      % Compile,
      mongodb.scalaDriver  % Compile,
      pagopa.catalogModels % Compile,
      pagopa.tenantModels  % Compile,
      pagopa.commons       % Compile,
      pagopa.cqrs          % Compile,
      pagopa.file          % Compile,
      pagopa.parser        % Compile
    ).map(_.withSources.withJavadoc)

    val certifiedMailSenderDependencies: Seq[ModuleID] =
      Seq(
        circe.core      % Compile,
        circe.generic   % Compile,
        circe.parser    % Compile,
        logback.classic % Compile,
        pagopa.commons  % Compile,
        pagopa.mail     % Compile,
        pagopa.queue    % Compile
      )
    
  lazy val eservicesMonitoringExporter: Seq[ModuleID] = Seq(
      cats.core                 % Compile,
      "com.github.pureconfig"   %% "pureconfig" % "0.17.2",
      logback.classic           % Compile,
      mongodb.scalaDriver       % Compile,
      pagopa.catalogModels      % Compile,
      pagopa.tenantModels       % Compile,
      pagopa.commons            % Compile,
      pagopa.cqrs               % Compile,
      pagopa.file               % Compile,
      circe.core                % Compile,
      circe.generic             % Compile
    ).map(_.withSources.withJavadoc)
    val privacyNoticesUpdaterDependencies: Seq[ModuleID] =
      Seq(
        "com.github.pureconfig" %% "pureconfig" % "0.17.2",
        logback.classic % Compile,
        cats.core       % Compile,
        pagopa.commons  % Compile,
        scanamo.scanamo % Compile,
        sttp.sttpClient % Compile,
        sttp.stpCirce   % Compile,
        sttp.sttpModel  % Compile,
        sttp.stpLog     % Compile,
        circe.generic   % Compile
    ).map(_.withSources.withJavadoc)
  }
}
