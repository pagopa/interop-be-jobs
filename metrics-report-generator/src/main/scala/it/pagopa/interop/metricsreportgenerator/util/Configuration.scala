package it.pagopa.interop.metricsreportgenerator.util

import cats.syntax.all._
import scala.concurrent.Future
import it.pagopa.interop.commons.cqrs.model.ReadModelConfig
import it.pagopa.interop.commons.mail.MailConfiguration
import it.pagopa.interop.commons.mail.MailConfiguration._
import pureconfig._
import pureconfig.generic.auto._
import pureconfig.error.ConfigReaderException
import javax.mail.internet.InternetAddress
import it.pagopa.interop.commons.mail.Mail
import pureconfig.error.ExceptionThrown

final case class Configuration(
  environment: String,
  recipients: List[InternetAddress],
  readModel: ReadModelConfig,
  collections: CollectionsConfiguration,
  tokens: TokensBucketConfiguration,
  metric: MetricConfiguration,
  interface: InterfaceConfiguration,
  mailer: MailConfiguration
)

final case class CollectionsConfiguration(
  limit: Int,
  tenants: String,
  agreements: String,
  purposes: String,
  eservices: String
)
final case class TokensBucketConfiguration(bucket: String, basePath: String)
final case class MetricConfiguration(bucket: String, basePath: String)
final case class InterfaceConfiguration(bucket: String)

object Configuration {
  implicit val overrideRecipientReader: ConfigReader[List[InternetAddress]] =
    ConfigReader.fromString[List[InternetAddress]](Mail.addresses(_).leftMap(ExceptionThrown))

  def read(): Future[Configuration] = ConfigSource.default
    .load[Configuration]
    .leftMap(ConfigReaderException(_))
    .fold(Future.failed, Future.successful)
}
