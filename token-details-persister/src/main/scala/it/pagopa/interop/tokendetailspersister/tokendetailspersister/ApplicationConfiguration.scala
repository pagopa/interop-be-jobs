package it.pagopa.interop.tokendetailspersister.tokendetailspersister

import com.typesafe.config.{Config, ConfigFactory}

object ApplicationConfiguration {
  val config: Config                  = ConfigFactory.load()
  val jwtQueueUrl: String             = config.getString("token-details-persister.queue.url")
  val maxNumberOfMessagesPerFile: Int = config.getInt("token-details-persister.queue.max-number-of-messages-per-file")
  val visibilityTimeout: Int          = config.getInt("token-details-persister.queue.visibility-timeout-in-seconds")

  val containerPath: String    = config.getString("token-details-persister.storage.container")
  val tokenStoragePath: String = config.getString("token-details-persister.storage.token-storage-path")
}
