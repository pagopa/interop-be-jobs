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
  readModel: ReadModelConfig,
  collections: CollectionsConfiguration,
  token: TokensBucketConfiguration
)

final case class CollectionsConfiguration(tenants: String, agreements: String, purposes: String, eservices: String)

final case class TokensBucketConfiguration(bucket: String, basePath: String)

object Configuration {
  def read(): Future[Configuration] =
    ConfigSource.default
      .load[Configuration]
      .leftMap(new ConfigReaderException(_))
      .fold(Future.failed, Future.successful)
}
