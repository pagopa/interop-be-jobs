package it.pagopa.interop.tokendetailspersister

import com.typesafe.scalalogging.Logger
import it.pagopa.interop.commons.files.service.FileManager
import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier
import it.pagopa.interop.commons.utils.service.impl.OffsetDateTimeSupplierImpl

import java.util.concurrent.{ExecutorService, Executors}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}

object Main extends App {
  private val logger: Logger               = Logger(this.getClass)
  val blockingThreadPool: ExecutorService  = Executors.newFixedThreadPool(Runtime.getRuntime.availableProcessors())
  val blockingEC: ExecutionContextExecutor = ExecutionContext.fromExecutor(blockingThreadPool)

  logger.info("Starting token details persister job")
  val fileManager: FileManager                 = FileManager.get(FileManager.S3)(blockingEC)
  val dateTimeSupplier: OffsetDateTimeSupplier = OffsetDateTimeSupplierImpl
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
