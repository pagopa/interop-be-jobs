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

  private[this] object pagopa {
    lazy val namespace = "it.pagopa"

    lazy val attributeRegistryManagement =
      namespace %% "interop-be-attribute-registry-management-client" % attributeRegistryManagementVersion

    lazy val commons = namespace %% "interop-commons-utils"         % commonsVersion
    lazy val jwt     = namespace %% "interop-commons-jwt"           % commonsVersion
    lazy val signer  = namespace %% "interop-commons-signer"        % commonsVersion
    lazy val queue   = namespace %% "interop-commons-queue-manager" % commonsVersion
    lazy val file    = namespace %% "interop-commons-file-manager"  % commonsVersion
  }

  object Jars {
    lazy val overrides: Seq[ModuleID]        =
      Seq(
        jackson.annotations % Compile,
        jackson.core        % Compile,
        jackson.databind    % Compile,
        jackson.scalaModule % Compile
      )
    lazy val attributesLoader: Seq[ModuleID] = Seq(
      // For making Java 12 happy
      "javax.annotation"                 % "javax.annotation-api" % "1.3.2" % "compile",
      //
      akka.actor                         % Compile,
      akka.actorTyped                    % Compile,
      akka.serialization                 % Compile,
      akka.slf4j                         % Compile,
      akka.stream                        % Compile,
      logback.classic                    % Compile,
      pagopa.attributeRegistryManagement % Compile,
      pagopa.commons                     % Compile,
      pagopa.jwt                         % Compile,
      pagopa.signer                      % Compile
    )

    lazy val tokenDetailsPersister: Seq[ModuleID] = Seq(
      // For making Java 12 happy
      "javax.annotation" % "javax.annotation-api" % "1.3.2" % "compile",
      //
      logback.classic    % Compile,
      pagopa.file        % Compile,
      pagopa.queue       % Compile
    )
  }
}
