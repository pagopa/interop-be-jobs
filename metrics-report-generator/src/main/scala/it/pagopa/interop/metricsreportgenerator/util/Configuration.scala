package it.pagopa.interop.metricsreportgenerator.util

import cats.syntax.either._
import scala.concurrent.Future
import it.pagopa.interop.commons.cqrs.model.ReadModelConfig
import it.pagopa.interop.commons.mail.MailConfiguration
import it.pagopa.interop.commons.mail.MailConfiguration._
import pureconfig._
import pureconfig.generic.auto._
import pureconfig.error.ConfigReaderException
import javax.mail.internet.InternetAddress

final case class Configuration(
  environment: String,
  recipients: List[InternetAddress],
  readModel: ReadModelConfig,
  collections: CollectionsConfiguration,
  tokens: TokensBucketConfiguration,
  storage: StorageConfiguration,
  mailer: MailConfiguration
)

final case class CollectionsConfiguration(
  maxParallelism: Int,
  tenants: String,
  agreements: String,
  purposes: String,
  eservices: String
)
final case class TokensBucketConfiguration(bucket: String, basePath: String)
final case class StorageConfiguration(bucket: String, basePath: String)

object Configuration {
  def read(): Future[Configuration] = ConfigSource.default
    .load[Configuration]
    .leftMap(new ConfigReaderException(_))
    .fold(Future.failed, Future.successful)
}
