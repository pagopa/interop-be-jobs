package it.pagopa.interop.privacynoticesupdater.util

import cats.syntax.either._
import pureconfig._
import pureconfig.generic.auto._
import pureconfig.error.ConfigReaderException
import scala.concurrent.Future

import java.util.UUID

final case class Configuration(dynamo: DynamoConfiguration, oneTrust: OneTrustConfiguration)

final case class DynamoConfiguration(tableName: String)

final case class OneTrustConfiguration(
  tokenUrl: String,
  privacyNoticesUrl: String,
  ppUuid: UUID,
  tosUuid: UUID,
  clientId: String,
  clientSecret: String
)

object Configuration {
  def read(): Future[Configuration] =
    ConfigSource.default
      .load[Configuration]
      .leftMap(new ConfigReaderException(_))
      .fold(Future.failed, Future.successful)
}
