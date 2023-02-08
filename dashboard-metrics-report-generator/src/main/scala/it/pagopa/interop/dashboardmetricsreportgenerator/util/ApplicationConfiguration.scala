package it.pagopa.interop.dashboardmetricsreportgenerator.util

import com.typesafe.config.{Config, ConfigFactory}
import it.pagopa.interop.commons.cqrs.model.ReadModelConfig

object ApplicationConfiguration {

  val config: Config = ConfigFactory.load()

  val agreementsContainer: String       = config.getString("report-generator.storage.agreements.container")
  val agreementsCsvStoragePath: String  = config.getString("report-generator.storage.agreements.csv.storage-path")
  val agreementsJsonStoragePath: String = config.getString("report-generator.storage.agreements.json.storage-path")

  val tenantCollection: String    = config.getString("report-generator.read-model.collections.tenants")
  val agreementCollection: String = config.getString("report-generator.read-model.collections.agreements")
  val purposeCollection: String   = config.getString("report-generator.read-model.collections.purposes")

  val readModelConfig: ReadModelConfig = {
    val connectionString: String = config.getString("report-generator.read-model.db.connection-string")
    val dbName: String           = config.getString("report-generator.read-model.db.name")

    ReadModelConfig(connectionString, dbName)
  }

}
