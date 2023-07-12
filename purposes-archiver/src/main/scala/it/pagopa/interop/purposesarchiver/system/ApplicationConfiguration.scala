package it.pagopa.interop.purposesarchiver.system

import com.typesafe.config.{Config, ConfigFactory}

object ApplicationConfiguration {

  private val config: Config = ConfigFactory.load()
  
  val purposesArchiverQueueName: String        = config.getString("queue.purposes-archiver-queue-name")
  val visibilityTimeoutInSeconds: Int          = config.getInt("queue.visibility-timeout-in-seconds")
  val agreementProcessURL: String              = config.getString("services.agreement-process")
  val purposeProcessURL: String                = config.getString("services.purpose-process")
}
