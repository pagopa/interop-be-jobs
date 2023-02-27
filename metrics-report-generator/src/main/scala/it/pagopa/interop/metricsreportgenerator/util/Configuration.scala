package it.pagopa.interop.metricsreportgenerator.util

import cats.syntax.either._
import scala.concurrent.Future
import it.pagopa.interop.commons.cqrs.model.ReadModelConfig
import pureconfig._
import pureconfig.generic.auto._
import pureconfig.error.ConfigReaderException

final case class Configuration(
  agreements: AgreementsConfiguration,
  readModel: ReadModelConfig,
  collections: CollectionsConfiguration
)

final case class AgreementsConfiguration(container: String, csvStoragePath: String, jsonStoragePath: String)
final case class CollectionsConfiguration(tenants: String, agreements: String, purposes: String)

object Configuration {
  def read(): Future[Configuration] =
    ConfigSource.default
      .load[Configuration]
      .leftMap(new ConfigReaderException(_))
      .fold(Future.failed, Future.successful)
}
