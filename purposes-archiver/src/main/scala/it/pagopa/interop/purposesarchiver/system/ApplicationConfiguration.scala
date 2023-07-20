package it.pagopa.interop.purposesarchiver.system

import com.typesafe.config.{Config, ConfigFactory}

object ApplicationConfiguration {

  private val config: Config = ConfigFactory.load()

  val purposesArchiverQueueName: String =
    config.getString("interop-be-purposes-archiver.queue.purposes-archiver-queue-name")
  val visibilityTimeoutInSeconds: Int   =
    config.getInt("interop-be-purposes-archiver.queue.visibility-timeout-in-seconds")
  val agreementProcessURL: String       = config.getString("interop-be-purposes-archiver.services.agreement-process")
  val purposeProcessURL: String         = config.getString("interop-be-purposes-archiver.services.purpose-process")

  val rsaKeysIdentifiers: Set[String] =
    config.getString("interop-be-purposes-archiver.rsa-keys-identifiers").split(",").toSet.filter(_.nonEmpty)

  val ecKeysIdentifiers: Set[String] =
    config.getString("interop-be-purposes-archiver.ec-keys-identifiers").split(",").toSet.filter(_.nonEmpty)

  val signerMaxConnections: Int = config.getInt("interop-be-purposes-archiver.signer-max-connections")

  require(
    rsaKeysIdentifiers.nonEmpty || ecKeysIdentifiers.nonEmpty,
    "You MUST provide at least one signing key (either RSA or EC)"
  )
}
