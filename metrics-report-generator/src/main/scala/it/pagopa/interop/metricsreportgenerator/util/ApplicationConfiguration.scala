package it.pagopa.interop.metricsreportgenerator.util

import com.typesafe.config.{Config, ConfigFactory}
import it.pagopa.interop.commons.cqrs.model.ReadModelConfig

object ApplicationConfiguration {

  val config: Config = ConfigFactory.load()

  val interfacesContainer: String = config.getString("report-generator.storage.interfaces-container")

  val metricsContainer: String   = config.getString("report-generator.storage.metrics-container")
  val metricsStoragePath: String = config.getString("report-generator.storage.metrics-storage-path")

  val catalogCollection: String = config.getString("report-generator.read-model.collections.eservices")
  val tenantCollection: String  = config.getString("report-generator.read-model.collections.tenants")

  val readModelConfig: ReadModelConfig = {
    val connectionString: String = config.getString("report-generator.read-model.db.connection-string")
    val dbName: String           = config.getString("report-generator.read-model.db.name")

    ReadModelConfig(connectionString, dbName)
  }

}
