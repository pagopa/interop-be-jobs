package it.pagopa.interop.metricsreportgenerator.util

import com.typesafe.config.{Config, ConfigFactory}

object ApplicationConfiguration {

  val config: Config = ConfigFactory.load()

  val interfacesContainer: String = config.getString("report-generator.storage.interfaces-container")

  val metricsContainer: String   = config.getString("report-generator.storage.metrics-container")
  val metricsStoragePath: String = config.getString("report-generator.storage.metrics-storage-path")

  val databaseURL: String = config.getString("report-generator.database.url")

  val databaseName: String = config.getString("report-generator.database.db")

  val catalogCollection: String = config.getString("report-generator.database.collections.eservices")

  val tenantCollection: String = config.getString("report-generator.database.collections.tenants")

}
