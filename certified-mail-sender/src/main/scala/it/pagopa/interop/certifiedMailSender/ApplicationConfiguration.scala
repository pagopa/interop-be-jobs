package it.pagopa.interop.certifiedMailSender

import com.typesafe.config.{Config, ConfigFactory}

object ApplicationConfiguration {
  val config: Config = ConfigFactory.load()

  val queueUrl: String         = config.getString("certified-mail-sender.queue.url")
  val maxConsumeBatchSize: Int = config.getInt("certified-mail-sender.queue.max-consume-batch-size")
  val visibilityTimeout: Int   = config.getInt("certified-mail-sender.queue.visibility-timeout-in-seconds")

  val serverAddress: String = config.getString("certified-mail-sender.smtp.address")
  val serverPort: Int       = config.getInt("certified-mail-sender.smtp.port")
  val smtpUser: String      = config.getString("certified-mail-sender.smtp.user")
  val smtpPassword: String  = config.getString("certified-mail-sender.smtp.password")
  val senderAddress: String = config.getString("certified-mail-sender.smtp.sender")

}
