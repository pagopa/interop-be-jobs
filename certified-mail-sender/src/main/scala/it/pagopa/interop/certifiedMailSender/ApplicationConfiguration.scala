package it.pagopa.interop.certifiedMailSender

import cats.implicits.catsSyntaxOptionId
import com.typesafe.config.{Config, ConfigFactory}
import it.pagopa.interop.certifiedMailSender.Error.SenderNotFound
import it.pagopa.interop.commons.mail.{Mail, MailConfiguration, SMTPConfiguration}

object ApplicationConfiguration {
  val config: Config = ConfigFactory.load()

  val queueUrl: String       = config.getString("certified-mail-sender.queue.url")
  val visibilityTimeout: Int = config.getInt("certified-mail-sender.queue.visibility-timeout-in-seconds")

  val mailConfiguration: Either[Throwable, MailConfiguration] = for {
    mails  <- Mail.from(config.getString("certified-mail-sender.smtp.sender"))
    sender <- mails.headOption.toRight(SenderNotFound)
  } yield MailConfiguration(
    sender = sender,
    smtp = SMTPConfiguration(
      user = config.getString("certified-mail-sender.smtp.user"),
      password = config.getString("certified-mail-sender.smtp.password"),
      serverAddress = config.getString("certified-mail-sender.smtp.address"),
      serverPort = config.getInt("certified-mail-sender.smtp.port"),
      authenticated = true.some,
      withTls = false.some,
      withSsl = config.getBoolean("certified-mail-sender.smtp.withSSL").some
    )
  )
}
