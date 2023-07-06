package it.pagopa.interop.attributesloader.system

import com.typesafe.config.{Config, ConfigFactory}
import it.pagopa.interop.commons.cqrs.model.ReadModelConfig

object ApplicationConfiguration {

  lazy val config: Config = ConfigFactory.load()

  lazy val attributeRegistryProcessURL: String =
    config.getString("interop-be-attributes-loader.services.attribute-registry-process")

  lazy val partyRegistryProxyURL: String =
    config.getString("interop-be-attributes-loader.services.party-registry-proxy")

  val rsaKeysIdentifiers: Set[String] =
    config.getString("interop-be-attributes-loader.rsa-keys-identifiers").split(",").toSet.filter(_.nonEmpty)

  val ecKeysIdentifiers: Set[String] =
    config.getString("interop-be-attributes-loader.ec-keys-identifiers").split(",").toSet.filter(_.nonEmpty)

  val signerMaxConnections: Int = config.getInt("interop-be-attributes-loader.signer-max-connections")

  val readModelConfig: ReadModelConfig = {
    val connectionString: String = config.getString("interop-be-attributes-loader.read-model.db.connection-string")
    val dbName: String           = config.getString("interop-be-attributes-loader.read-model.db.name")

    ReadModelConfig(connectionString, dbName)
  }

  require(
    rsaKeysIdentifiers.nonEmpty || ecKeysIdentifiers.nonEmpty,
    "You MUST provide at least one signing key (either RSA or EC)"
  )
}
