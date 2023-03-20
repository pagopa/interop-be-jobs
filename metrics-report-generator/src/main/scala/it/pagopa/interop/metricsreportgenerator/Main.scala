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

object Main extends App {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass.getCanonicalName())

  implicit val context: List[(String, String)] = (CORRELATION_ID_HEADER -> UUID.randomUUID().toString()) :: Nil

  logger.info("Starting metrics report generator job")

  val blockingThreadPool: ExecutorService = Executors.newFixedThreadPool(Runtime.getRuntime.availableProcessors())

  implicit val blockingEC: ExecutionContextExecutor     = ExecutionContext.fromExecutor(blockingThreadPool)
  implicit val fileManager: FileManager                 = FileManager.get(FileManager.S3)(blockingEC)
  implicit val readModelService: ReadModelService       = new MongoDbReadModelService(
    ApplicationConfiguration.readModelConfig
  )
  implicit val dateTimeSupplier: OffsetDateTimeSupplier = OffsetDateTimeSupplier

  val fileUtils: FileUtils     = new FileUtils(fileManager, dateTimeSupplier)
  val fileWriters: FileWriters = new FileWriters(fileUtils, dateTimeSupplier)

  def execution(): Future[Unit] = for {
    activeAgreements <- Utils.retrieveAllActiveAgreements(ReadModelQueries.getActiveAgreements, 0, Seq.empty)
    purposes         <- Utils.retrieveAllPurposes(ReadModelQueries.getPurposes, 0, Seq.empty)
    agreementRecords = AgreementRecord.join(activeAgreements, purposes)
    _ <- fileWriters.agreementsJsonWriter(agreementRecords)
    _ <- fileWriters.agreementsCsvWriter(agreementRecords)
  } yield ()

  def run(): Future[Unit] = execution()
    .recover(ex => logger.error("There was an error while running the job", ex))(global)
    .andThen { _ =>
      fileManager.close()
      readModelService.close()
      blockingThreadPool.shutdown()
    }(global)

  Await.result(run(), Duration.Inf): Unit
  logger.info("Completed metrics report generator job")
}
