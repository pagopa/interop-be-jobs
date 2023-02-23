package it.pagopa.interop.dashboardmetricsreportgenerator

import cats.syntax.either._
import it.pagopa.interop.commons.cqrs.model.ReadModelConfig
import pureconfig._
import pureconfig.generic.auto._
import pureconfig.error.ConfigReaderException
import scala.concurrent.Future

final case class Configuration(
  storage: StorageBucketConfiguration,
  tokensStorage: TokensBucketConfiguration,
  readModel: ReadModelConfig,
  collections: CollectionsConfiguraion,
  partyManagement: PartyManagementConfiguration,
  overrides: Overrides
)
final case class StorageBucketConfiguration(bucket: String, filename: String)
final case class TokensBucketConfiguration(bucket: String, basePath: String)
final case class CollectionsConfiguraion(tenants: String, agreements: String, purposes: String, eservices: String)
final case class PartyManagementConfiguration(
  partyManagementUrl: String,
  partyManagementApiKey: String,
  interopProductName: String
)
final case class Overrides(totalTenants: Option[Int])

object Configuration {
  def read(): Future[Configuration] =
    ConfigSource.default
      .load[Configuration]
      .leftMap(new ConfigReaderException(_))
      .fold(Future.failed, Future.successful)
}
