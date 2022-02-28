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

  private[this] object pagopa {
    lazy val namespace = "it.pagopa"

    lazy val attributeRegistryManagement =
      namespace %% "interop-be-attribute-registry-management-client" % attributeRegistryManagementVersion

    lazy val commons = namespace %% "interop-commons-utils" % commonsVersion
    lazy val jwt     = namespace %% "interop-commons-jwt"   % commonsVersion
    lazy val vault   = namespace %% "interop-commons-vault" % commonsVersion
  }

  private[this] object logback {
    lazy val namespace = "ch.qos.logback"
    lazy val classic   = namespace % "logback-classic" % logbackVersion
  }

  private[this] object kamon {
    lazy val namespace  = "io.kamon"
    lazy val bundle     = namespace %% "kamon-bundle"     % kamonVersion
    lazy val prometheus = namespace %% "kamon-prometheus" % kamonVersion
  }

  object Jars {
    lazy val cli: Seq[ModuleID] = Seq(
      // For making Java 12 happy
      "javax.annotation" % "javax.annotation-api" % "1.3.2" % "compile",
      //
      akka.actor                         % Compile,
      akka.actorTyped                    % Compile,
      akka.serialization                 % Compile,
      akka.slf4j                         % Compile,
      akka.stream                        % Compile,
      kamon.bundle                       % Compile,
      kamon.prometheus                   % Compile,
      logback.classic                    % Compile,
      pagopa.attributeRegistryManagement % Compile,
      pagopa.commons                     % Compile,
      pagopa.jwt                         % Compile,
      pagopa.vault                       % Compile
    )
  }
}
