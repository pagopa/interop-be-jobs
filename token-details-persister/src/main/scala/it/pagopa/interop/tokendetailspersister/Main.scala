package it.pagopa.interop.tokendetailspersister

import com.typesafe.scalalogging.Logger
import it.pagopa.interop.commons.files.service.FileManager
import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier

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

  val blockingThreadPool: ExecutorService  = Executors.newFixedThreadPool(Runtime.getRuntime.availableProcessors())
  val blockingEC: ExecutionContextExecutor = ExecutionContext.fromExecutor(blockingThreadPool)

  logger.info("Starting token details persister job")
  val fileManager: FileManager                 = FileManager.get(FileManager.S3)(blockingEC)
  val dateTimeSupplier: OffsetDateTimeSupplier = OffsetDateTimeSupplier
  val fileUtils: FileUtils                     = new FileUtils(fileManager, dateTimeSupplier)
  val job: JobExecution                        = JobExecution(fileUtils)(blockingEC)
  val execution: Future[Unit]                  = job
    .run()
    .recover { ex =>
      logger.error("There was an error while running the job", ex)
    }(global)
    .andThen(_ => blockingThreadPool.shutdown())

  Await.result(execution, Duration.Inf)
  logger.info("Completed token details persister job")
}
