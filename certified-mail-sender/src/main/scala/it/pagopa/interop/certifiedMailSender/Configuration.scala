package it.pagopa.interop.certifiedMailSender

import cats.syntax.either._
import it.pagopa.interop.commons.mail.MailConfiguration
import it.pagopa.interop.commons.mail.MailConfiguration._
import it.pagopa.interop.commons.utils.TypeConversions.EitherOps
import pureconfig._
import pureconfig.error.ConfigReaderException
import pureconfig.generic.auto._

import scala.concurrent.Future

final case class QueueConfiguration(url: String, maxConsumeBatchSize: Int, visibilityTimeoutInSeconds: Int)

final case class Configuration(queue: QueueConfiguration, mail: MailConfiguration)
object Configuration {
  def read(): Future[Configuration] =
    ConfigSource.default
      .load[Configuration]
      .leftMap(ConfigReaderException(_))
      .toFuture

}
