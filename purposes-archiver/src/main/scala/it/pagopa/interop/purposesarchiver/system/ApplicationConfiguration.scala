package it.pagopa.interop.purposesarchiver.system

import com.typesafe.config.{Config, ConfigFactory}
import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier

object ApplicationConfiguration {

  private val config: Config = ConfigFactory.load()

  val dateTimeSupplier: OffsetDateTimeSupplier = OffsetDateTimeSupplier
  val purposesArchiverQueueName: String        = config.getString("purposes-archiver-queue-name")
  val visibilityTimeoutInSeconds: Int          = config.getInt("visibility-timeout-in-seconds")
  val agreementProcessURL: String              = config.getString("services.agreement-process")
  val purposeProcessURL: String                = config.getString("services.purpose-process")
}
