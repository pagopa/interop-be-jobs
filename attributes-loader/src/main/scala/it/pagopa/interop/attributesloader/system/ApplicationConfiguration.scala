package it.pagopa.interop.attributesloader.system

import com.typesafe.config.{Config, ConfigFactory}

object ApplicationConfiguration {

  lazy val config: Config = ConfigFactory.load()

  lazy val attributeRegistryManagementURL: String =
    config.getString("interop-be-attributes-loader.services.attribute-registry-management")

  val rsaKeysIdentifiers: Set[String] =
    config.getString("interop-be-attributes-loader.rsa-keys-identifiers").split(",").toSet.filter(_.nonEmpty)

  val ecKeysIdentifiers: Set[String] =
    config.getString("interop-be-attributes-loader.ec-keys-identifiers").split(",").toSet.filter(_.nonEmpty)

  val signerMaxConnections: Int = config.getInt("interop-be-attributes-loader.signer-max-connections")

  require(
    rsaKeysIdentifiers.nonEmpty || ecKeysIdentifiers.nonEmpty,
    "You MUST provide at least one signing key (either RSA or EC)"
  )
}
