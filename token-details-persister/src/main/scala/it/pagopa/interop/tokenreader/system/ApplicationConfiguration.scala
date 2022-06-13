package it.pagopa.interop.tokenreader.system

import com.typesafe.config.{Config, ConfigFactory}

object ApplicationConfiguration {

  val config: Config = ConfigFactory.load()

  val maxNumberOfMessagesPerFile: Int = config.getInt("token-details-persister.max-number-of-messages-per-file")

  val batchSize: Int = config.getInt("token-details-persister.queue.batch-size")

  val visibilityTimeout: Int = config.getInt("token-details-persister.queue.visibility-timeout")

  val storageContainer: String = config.getString("token-details-persister.storage.container")
  val tokenStoragePath: String = config.getString("token-details-persister.storage.token-details-path")

  val jwtQueueUrl: String = config.getString("token-details-persister.jwt-queue-url")

  require(batchSize <= 10, """Max value for "batch-size" is 10.""")
}
