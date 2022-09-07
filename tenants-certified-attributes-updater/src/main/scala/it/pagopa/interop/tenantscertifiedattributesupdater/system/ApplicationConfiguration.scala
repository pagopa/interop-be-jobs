package it.pagopa.interop.tenantscertifiedattributesupdater.system

import com.typesafe.config.{Config, ConfigFactory}

object ApplicationConfiguration {

  val config: Config = ConfigFactory.load()

  val partyRegistryProxyURL: String =
    config.getString("interop-be-tenants-certified-attributes-updater.services.party-registry-proxy")

  val tenantProcessURL: String =
    config.getString("interop-be-tenants-certified-attributes-updater.services.tenant-process")

  val attributesDatabaseURL: String =
    config.getString("interop-be-tenants-certified-attributes-updater.databases.attributes.url")

  val attributesDatabase: String =
    config.getString("interop-be-tenants-certified-attributes-updater.databases.attributes.db")

  val attributesCollection: String =
    config.getString("interop-be-tenants-certified-attributes-updater.databases.attributes.collection")

  val tenantsDatabaseUrl: String =
    config.getString("interop-be-tenants-certified-attributes-updater.databases.tenants.url")

  val tenantsDatabase: String =
    config.getString("interop-be-tenants-certified-attributes-updater.databases.tenants.db")

  val tenantsCollection: String =
    config.getString("interop-be-tenants-certified-attributes-updater.databases.tenants.collection")

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

  val signerMaxConnections: Int =
    config.getInt("interop-be-tenants-certified-attributes-updater.signer-max-connections")

  require(
    rsaKeysIdentifiers.nonEmpty || ecKeysIdentifiers.nonEmpty,
    "You MUST provide at least one signing key (either RSA or EC)"
  )
}
