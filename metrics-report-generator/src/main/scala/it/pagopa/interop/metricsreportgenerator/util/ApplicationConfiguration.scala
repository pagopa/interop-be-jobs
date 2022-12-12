package it.pagopa.interop.metricsreportgenerator.util

import com.typesafe.config.{Config, ConfigFactory}

object ApplicationConfiguration {

  val config: Config = ConfigFactory.load()

  val partyRegistryProxyURL: String = config.getString("report-generator.services.party-registry-proxy")

  val containerPath: String = config.getString("report-generator.storage.container")

  val databaseURL: String = config.getString("report-generator.database.url")

  val databaseName: String = config.getString("report-generator.database.db")

  val agreementCollection: String = config.getString("report-generator.database.collections.agreements")

  val catalogCollection: String = config.getString("report-generator.database.collections.eservices")

  val tenantCollection: String = config.getString("report-generator.database.collections.tenants")

  val rsaKeysIdentifiers: Set[String] = config
    .getString("report-generator.rsa-keys-identifiers")
    .split(",")
    .toSet
    .filter(_.nonEmpty)

  val ecKeysIdentifiers: Set[String] =
    config
      .getString("report-generator.ec-keys-identifiers")
      .split(",")
      .toSet
      .filter(_.nonEmpty)

  require(
    rsaKeysIdentifiers.nonEmpty || ecKeysIdentifiers.nonEmpty,
    "You MUST provide at least one signing key (either RSA or EC)"
  )
}
