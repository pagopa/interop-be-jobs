package it.pagopa.interop.metricsreportgenerator

import com.typesafe.scalalogging.Logger
import it.pagopa.interop.commons.cqrs.service.{MongoDbReadModelService, ReadModelService}
import it.pagopa.interop.commons.files.service.FileManager
import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier
import it.pagopa.interop.metricsreportgenerator.report.AgreementRecord
import it.pagopa.interop.metricsreportgenerator.util._

import java.util.concurrent.{ExecutorService, Executors}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}
import java.util.UUID
import it.pagopa.interop.commons.logging._
import com.typesafe.scalalogging.LoggerTakingImplicit
import it.pagopa.interop.commons.utils.CORRELATION_ID_HEADER
import it.pagopa.interop.commons.cqrs.model.ReadModelConfig

object Main extends App {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass.getCanonicalName())

  implicit val context: List[(String, String)] = (CORRELATION_ID_HEADER -> UUID.randomUUID().toString()) :: Nil

  logger.info("Starting metrics report generator job")

  val blockingThreadPool: ExecutorService  =
    Executors.newFixedThreadPool(1.max(Runtime.getRuntime.availableProcessors() - 1))
  val blockingEC: ExecutionContextExecutor = ExecutionContext.fromExecutor(blockingThreadPool)
  val fileManager: FileManager             = FileManager.get(FileManager.S3)(blockingEC)

  implicit val readModelService: ReadModelService = new MongoDbReadModelService(ReadModelConfig("", ""))

  val fileUtils: FileUtils = new FileUtils(fileManager, OffsetDateTimeSupplier)

  def execution(): Future[Unit] = for {
    config           <- Configuration.read()
    activeAgreements <- ReadModelQueries.getAllActiveAgreements(100)(config.collections)
    purposes         <- ReadModelQueries.getAllPurposes(100)(config.collections)
    agreementRecords = AgreementRecord.join(activeAgreements, purposes)
    _ <- fileUtils.store(config.agreements)(AgreementRecord.csv(agreementRecords))
  } yield ()

  def run(): Future[Unit] = execution()
    .recover(ex => logger.error("There was an error while running the job", ex))(global)
    .andThen { _ =>
      fileManager.close()
      readModelService.close()
      blockingThreadPool.shutdown()
    }(global)

  Await.result(run(), Duration.Inf)
  logger.info("Completed metrics report generator job")
}
