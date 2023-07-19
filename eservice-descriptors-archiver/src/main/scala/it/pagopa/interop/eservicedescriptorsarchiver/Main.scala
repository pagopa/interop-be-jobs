package it.pagopa.interop.eservicedescriptorsarchiver

import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.cqrs.service.MongoDbReadModelService
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.queue.config.SQSHandlerConfig
import it.pagopa.interop.commons.queue.impl.SQSHandler
import it.pagopa.interop.commons.utils.CORRELATION_ID_HEADER
import it.pagopa.interop.eservicedescriptorsarchiver.service.impl.CatalogProcessServiceImpl
import it.pagopa.interop.eservicedescriptorsarchiver.util.JobExecution

import java.util.UUID
import java.util.concurrent.{ExecutorService, Executors}
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

object Main extends App {

  implicit val context: List[(String, String)]     = (CORRELATION_ID_HEADER -> UUID.randomUUID().toString) :: Nil
  private val blockingThreadPool: ExecutorService  =
    Executors.newFixedThreadPool(Runtime.getRuntime.availableProcessors())
  private val blockingEC: ExecutionContextExecutor = ExecutionContext.fromExecutor(blockingThreadPool)
  implicit val ec: ExecutionContext                = blockingEC

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass.getCanonicalName)

  private val readModelService: MongoDbReadModelService = new MongoDbReadModelService(
    ApplicationConfiguration.readModelConfig
  )

  private val sqsHandlerConfig: SQSHandlerConfig =
    SQSHandlerConfig(
      queueUrl = ApplicationConfiguration.archivingEservicesQueueUrl,
      visibilityTimeout = ApplicationConfiguration.visibilityTimeout
    )

  private val sqsHandler: SQSHandler =
    SQSHandler(sqsHandlerConfig)(blockingEC)

  private val job: JobExecution = JobExecution(readModelService, CatalogProcessServiceImpl(blockingEC))

  logger.info("Starting eservice versions archiver job")

  private val execution: Future[Unit] =
    sqsHandler.processAllRawMessages(ApplicationConfiguration.maxNumberOfMessagesPerFile) {
      (messages: List[String], receipts: List[String]) =>
        logger.info(s"Saving ${messages.size} messages")
        for {
          _ <- Future.traverse(messages)(job.archiveEservice)
          _ <- Future.traverse(receipts)(sqsHandler.deleteMessage)
        } yield ()
    }

  private def executionLoop(): Future[Unit] = execution.flatMap(_ => executionLoop())

  private val init: Future[Unit] = executionLoop()
    .andThen {
      case Failure(e) => logger.error("Eservice version archiver job failed with exception", e)
      case Success(_) => logger.info("Completed eservice version archiver job")
    }
    .andThen { _ =>
      readModelService.close()
    }

  Await.ready(init, Duration.Inf)
}
