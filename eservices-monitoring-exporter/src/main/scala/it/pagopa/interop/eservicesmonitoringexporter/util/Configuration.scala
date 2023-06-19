package it.pagopa.interop.eservicesmonitoringexporter.util

import cats.syntax.all._
import it.pagopa.interop.commons.cqrs.model.ReadModelConfig
import pureconfig._
import pureconfig.generic.auto._
import pureconfig.error.{CannotConvert, ConfigReaderException}

import java.util.UUID
import scala.concurrent.Future
import scala.util.Try

final case class Configuration(
  storage: StorageBucketConfiguration,
  readModel: ReadModelConfig,
  collections: CollectionsConfiguration,
  producersAllowList: Option[List[UUID]]
)
final case class StorageBucketConfiguration(kind: String, bucket: String, filename: String, asndjson: Boolean)
final case class CollectionsConfiguration(eservices: String, tenants: String)

object Configuration {
  implicit val stringListReader: ConfigReader[List[UUID]] = ConfigReader.fromString { str =>
    str
      .split(",")
      .filter(_.nonEmpty)
      .map(s => Try(UUID.fromString(s.trim)).toEither.leftMap(ex => CannotConvert(s, "UUID", ex.getMessage)))
      .toList
      .sequence
  }

  def read(): Future[Configuration] =
    ConfigSource.default
      .load[Configuration]
      .leftMap(new ConfigReaderException(_))
      .fold(Future.failed, Future.successful)
}
