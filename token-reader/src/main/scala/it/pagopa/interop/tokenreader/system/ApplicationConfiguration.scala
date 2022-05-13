package it.pagopa.interop.tokenreader.system

import com.typesafe.config.{Config, ConfigFactory}

object ApplicationConfiguration {

  lazy val config: Config = ConfigFactory.load()

  val batchSize: Long =
    config.getLong("token-reader.batch-size")

  val maxNumberOfMessages: Int =
    config.getInt("token-reader.queue.number-of-messages")

  val visibilityTimeout: Int =
    config.getInt("token-reader.queue.visibility-timeout")

  val storageContainer: String = config.getString("token-reader.storage.container")
  val tokenStoragePath: String = config.getString("token-reader.storage.eservice-docs-path")

  val jwtQueueUrl: String = config.getString("token-reader.jwt-queue-url")

  require(maxNumberOfMessages <= 10, """Max value for "queue-number-of-messages" is 10.""")
}
