package it.pagopa.interop.eservicesmonitoringexporter

import com.typesafe.scalalogging.Logger
import it.pagopa.interop.commons.logging._
import com.typesafe.scalalogging.LoggerTakingImplicit
import it.pagopa.interop.commons.files.service.FileManager
import it.pagopa.interop.commons.cqrs.model.ReadModelConfig
import it.pagopa.interop.commons.cqrs.service.{MongoDbReadModelService, ReadModelService}
import it.pagopa.interop.commons.utils.CORRELATION_ID_HEADER
import it.pagopa.interop.eservicesmonitoringexporter.util.Jobs
import it.pagopa.interop.eservicesmonitoringexporter.util.Utils._
import it.pagopa.interop.eservicesmonitoringexporter.util.Configuration

import java.util.concurrent.{ExecutorService, Executors}
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, ExecutionContext, Future}

import java.util.UUID
import scala.util.Failure

object Main extends App {

  val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass.getCanonicalName())

  implicit val context: List[(String, String)] = (CORRELATION_ID_HEADER -> UUID.randomUUID().toString()) :: Nil

  logger.info("Starting eservices monitoring exporter job")

  def getFileManager(configuration: Configuration): Future[(FileManager, ExecutorService)] = Future {
    val blockingThreadPool: ExecutorService = Executors.newFixedThreadPool(Runtime.getRuntime.availableProcessors())
    (
      FileManager.get(configuration.storage.kind match {
        case "S3"   => FileManager.S3
        case "file" => FileManager.File
        case _      => throw new Exception("Incorrect File Manager")
      })(ExecutionContext.fromExecutor(blockingThreadPool)),
      blockingThreadPool
    )
  }

  def getReadModel(readModelConfig: ReadModelConfig): Future[ReadModelService] =
    Future(new MongoDbReadModelService(readModelConfig))

  def resources(): Future[(Configuration, FileManager, ReadModelService, ExecutorService)] =
    for {
      config   <- Configuration.read()
      (fm, es) <- getFileManager(config)
      rm       <- getReadModel(config.readModel)
    } yield (config, fm, rm, es)

  def execution(configuration: Configuration, fm: FileManager, rm: ReadModelService): Future[Unit] = {
    for {
      eServicesDB <- Jobs.getEservices()(global, rm)
      eServices = eServicesDB.flatMap(_.toPersistent)
      path <- Jobs.saveIntoBucket(fm)(eServices)(configuration, logger, context)
      _ = logger.info(s"Stored eservices json file at $path")
    } yield ()
  }

  def app(): Future[Unit] = resources()
    .flatMap { case (config, fm, rm, es) =>
      execution(config, fm, rm)
        .andThen { case Failure(e) =>
          logger.error("Error during eservices monitoring exporter job", e)
        }
        .andThen { _ =>
          fm.close()
          rm.close()
          es.shutdown()
        }
    }

  Await.ready(app(), Duration.Inf): Unit
  logger.info("Completed eservices monitoring exporter job")
}
