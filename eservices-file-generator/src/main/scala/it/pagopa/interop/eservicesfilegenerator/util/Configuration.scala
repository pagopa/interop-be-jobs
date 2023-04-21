package it.pagopa.interop.eservicesfilegenerator.util

import cats.syntax.either._
import it.pagopa.interop.commons.cqrs.model.ReadModelConfig
import pureconfig._
import pureconfig.generic.auto._
import pureconfig.error.ConfigReaderException
import scala.concurrent.Future

final case class Configuration(storage: StorageBucketConfiguration, readModel: ReadModelConfig)
final case class StorageBucketConfiguration(kind: String, bucket: String, filename: String)

object Configuration {
  def read(): Future[Configuration] =
    ConfigSource.default
      .load[Configuration]
      .leftMap(new ConfigReaderException(_))
      .fold(Future.failed, Future.successful)
}
