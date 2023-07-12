package it.pagopa.interop.eserviceversionsarchiver

import com.typesafe.config.{Config, ConfigFactory}
import it.pagopa.interop.commons.cqrs.model.ReadModelConfig
import it.pagopa.interop.commons.utils.CORRELATION_ID_HEADER

import java.util.UUID
import java.util.concurrent.{ExecutorService, Executors}
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

object ApplicationConfiguration {

  implicit val context: List[(String, String)]    = (CORRELATION_ID_HEADER -> UUID.randomUUID().toString) :: Nil
  private val blockingThreadPool: ExecutorService =
    Executors.newFixedThreadPool(Runtime.getRuntime.availableProcessors())
  val blockingEC: ExecutionContextExecutor        = ExecutionContext.fromExecutor(blockingThreadPool)
  implicit val ec: ExecutionContext               = blockingEC
  private val config: Config                      = ConfigFactory.load()

  val archivingPurposesQueueUrl: String =
    config.getString("eservice-versions-archiver.queue.archiving-purposes-queue-url")
  val maxNumberOfMessagesPerFile: Int   =
    config.getInt("eservice-versions-archiver.queue.max-number-of-messages-per-file")
  val visibilityTimeout: Int = config.getInt("eservice-versions-archiver.queue.visibility-timeout-in-seconds")

  val agreementsCollection: String = config.getString("read-model.collections.agreements")
  val eservicesCollection: String  = config.getString("read-model.collections.eservices")
  val catalogProcessURL: String    = config.getString("read-model.services.catalog-process")

  val readModelConfig: ReadModelConfig = {
    val connectionString: String = config.getString("read-model.connection-string")
    val dbName: String           = config.getString("read-model.name")

    ReadModelConfig(connectionString, dbName)
  }
}
