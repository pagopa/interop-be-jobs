package it.pagopa.interop.tenantsattributeschecker

import com.typesafe.config.{Config, ConfigFactory}
import it.pagopa.interop.commons.cqrs.model.ReadModelConfig
import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier

object ApplicationConfiguration {

  val config: Config = ConfigFactory.load()

  val dateTimeSupplier: OffsetDateTimeSupplier = OffsetDateTimeSupplier

  val agreementsCollection: String =
    config.getString("read-model.collections.agreements")

  val agreementProcessURL: String =
    config.getString("services.agreement-process")

  val attributesCollection: String =
    config.getString("read-model.collections.attributes")

  val tenantsCollection: String =
    config.getString("read-model.collections.tenants")

  val eservicesCollection: String =
    config.getString("read-model.collections.eservices")

  val readModelConfig: ReadModelConfig = {
    val connectionString: String = config.getString("read-model.connection-string")
    val dbName: String           = config.getString("read-model.name")

    ReadModelConfig(connectionString, dbName)
  }
}
