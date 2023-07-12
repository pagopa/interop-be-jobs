package it.pagopa.interop.eserviceversionsarchiver

import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.cqrs.service.MongoDbReadModelService
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.queue.impl.SQSHandler
import it.pagopa.interop.eserviceversionsarchiver.ApplicationConfiguration.{blockingEC, context, ec}
import it.pagopa.interop.eserviceversionsarchiver.service.impl.CatalogProcessServiceImpl
import it.pagopa.interop.eserviceversionsarchiver.util.JobExecution
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

object Main extends App {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass.getCanonicalName)

  private val readModelService: MongoDbReadModelService = new MongoDbReadModelService(
    ApplicationConfiguration.readModelConfig
  )
  private val sqsHandler: SQSHandler                    =
    SQSHandler(ApplicationConfiguration.archivingPurposesQueueUrl)(blockingEC)

  private val job: JobExecution = JobExecution(readModelService, CatalogProcessServiceImpl(blockingEC))

  logger.info("Starting eservice versions archiver job")

  private val execution: Future[Unit] = sqsHandler.processAllRawMessages(
    ApplicationConfiguration.maxNumberOfMessagesPerFile,
    ApplicationConfiguration.visibilityTimeout
  ) { (messages: List[String], receipts: List[String]) =>
    logger.info(s"Saving ${messages.size} messages")
    for {
      _ <- Future.traverse(messages)(job.archiveEservice)
      _ <- Future.traverse(receipts)(sqsHandler.deleteMessage)
    } yield ()
  }

  private val init: Future[Unit] = execution
    .andThen {
      case Failure(e) => logger.info("Eservice version archiver job failed with exception", e)
      case Success(_) => logger.info("Completed eservice version archiver job")
    }
    .andThen { _ =>
      readModelService.close()
    }

  Await.ready(init, Duration.Inf)
}
