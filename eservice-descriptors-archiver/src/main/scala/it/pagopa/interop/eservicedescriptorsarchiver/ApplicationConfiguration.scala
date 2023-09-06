package it.pagopa.interop.eservicedescriptorsarchiver

import com.typesafe.config.{Config, ConfigFactory}
import it.pagopa.interop.commons.cqrs.model.ReadModelConfig

object ApplicationConfiguration {

  private val config: Config = ConfigFactory.load()

  val archivingEservicesQueueUrl: String = config.getString("queue.archiving-eservices-queue-url")
  val visibilityTimeout: Int             = config.getInt("queue.visibility-timeout-in-seconds")

  val agreementsCollection: String = config.getString("read-model.collections.agreements")
  val eservicesCollection: String  = config.getString("read-model.collections.eservices")
  val catalogProcessURL: String    = config.getString("services.catalog-process")

  val readModelConfig: ReadModelConfig = {
    val connectionString: String = config.getString("read-model.connection-string")
    val dbName: String           = config.getString("read-model.name")

    ReadModelConfig(connectionString, dbName)
  }

  val rsaKeysIdentifiers: Set[String] = config
    .getString("key.rsa-keys-identifiers")
    .split(",")
    .toSet
    .filter(_.nonEmpty)

  val ecKeysIdentifiers: Set[String] = config
    .getString("key.ec-keys-identifiers")
    .split(",")
    .toSet
    .filter(_.nonEmpty)
}
