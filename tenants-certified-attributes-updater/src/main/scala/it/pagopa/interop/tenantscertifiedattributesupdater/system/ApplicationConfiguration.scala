package it.pagopa.interop.tenantscertifiedattributesupdater.system

import com.typesafe.config.{Config, ConfigFactory}

object ApplicationConfiguration {

  val config: Config = ConfigFactory.load()

  val partyRegistryProxyURL: String =
    config.getString("interop-be-tenants-certified-attributes-updater.services.party-registry-proxy")

  val tenantProcessURL: String =
    config.getString("interop-be-tenants-certified-attributes-updater.services.tenant-process")

  val databaseURL: String =
    config.getString("interop-be-tenants-certified-attributes-updater.database.url")

  val databaseName: String =
    config.getString("interop-be-tenants-certified-attributes-updater.database.db")

  val attributesCollection: String =
    config.getString("interop-be-tenants-certified-attributes-updater.database.collections.attributes")

  val tenantsCollection: String =
    config.getString("interop-be-tenants-certified-attributes-updater.database.collections.tenants")

  val rsaKeysIdentifiers: Set[String] =
    config
      .getString("interop-be-tenants-certified-attributes-updater.rsa-keys-identifiers")
      .split(",")
      .toSet
      .filter(_.nonEmpty)

  val ecKeysIdentifiers: Set[String] =
    config
      .getString("interop-be-tenants-certified-attributes-updater.ec-keys-identifiers")
      .split(",")
      .toSet
      .filter(_.nonEmpty)

  val ivassAssuranceAttributesCode: String =
    config.getString("interop-be-tenants-certified-attributes-updater.ivass-assurance-attribute-code")

  require(
    rsaKeysIdentifiers.nonEmpty || ecKeysIdentifiers.nonEmpty,
    "You MUST provide at least one signing key (either RSA or EC)"
  )
}
