package it.pagopa.interop.metricsreportgenerator.util

import cats.syntax.either._
import scala.concurrent.Future
import it.pagopa.interop.commons.cqrs.model.ReadModelConfig
import pureconfig._
import pureconfig.generic.auto._
import pureconfig.error.ConfigReaderException

trait ContainerConfiguration {
  val container: String
  val path: String
}

final case class Configuration(
  agreements: AgreementsConfiguration,
  readModel: ReadModelConfig,
  collections: CollectionsConfiguration
)

final case class AgreementsConfiguration(container: String, path: String) extends ContainerConfiguration

final case class CollectionsConfiguration(tenants: String, agreements: String, purposes: String, eservices: String)

object Configuration {
  def read(): Future[Configuration] =
    ConfigSource.default
      .load[Configuration]
      .leftMap(new ConfigReaderException(_))
      .fold(Future.failed, Future.successful)
}
