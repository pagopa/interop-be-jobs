package it.pagopa.interop.eservicesmonitoringexporter.util

import cats.syntax.either._
import it.pagopa.interop.commons.cqrs.model.ReadModelConfig
import pureconfig._
import pureconfig.generic.auto._
import pureconfig.error.ConfigReaderException
import scala.concurrent.Future

final case class Configuration(
  storage: StorageBucketConfiguration,
  readModel: ReadModelConfig,
  collections: CollectionsConfiguration
)
final case class StorageBucketConfiguration(kind: String, bucket: String, filename: String, asndjson: Boolean)
final case class CollectionsConfiguration(eservices: String, tenants: String)

object Configuration {
  def read(): Future[Configuration] =
    ConfigSource.default
      .load[Configuration]
      .leftMap(new ConfigReaderException(_))
      .fold(Future.failed, Future.successful)
}