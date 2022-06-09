package it.pagopa.interop.tokenreader.system

import com.typesafe.config.{Config, ConfigFactory}

object ApplicationConfiguration {

  val config: Config = ConfigFactory.load()

  val maxNumberOfMessagesPerFile: Int = config.getInt("token-reader.max-number-of-messages-per-file")

  val batchSize: Long = config.getLong("token-reader.queue.batch-size")

  val visibilityTimeout: Int = config.getInt("token-reader.queue.visibility-timeout")

  val storageContainer: String = config.getString("token-reader.storage.container")
  val tokenStoragePath: String = config.getString("token-reader.storage.token-details-path")

  val jwtQueueUrl: String = config.getString("token-reader.jwt-queue-url")

  require(maxNumberOfMessagesPerFile <= 10, """Max value for "max-number-of-messages-per-file" is 10.""")
}
