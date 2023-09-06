package it.pagopa.interop.eservicedescriptorsarchiver

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

object Main extends App with Dependencies {

  final val chunkSize: Int = 10

  logger.info("Starting eservice versions archiver job")

  private val execution: Future[Unit] = sqsHandler.processAllRawMessages(chunkSize)(processMessages)

  private def executionLoop(): Future[Unit] = execution.flatMap(_ => executionLoop())

  private val init: Future[Unit] = executionLoop()
    .andThen {
      case Failure(e) => logger.error("Eservice version archiver job failed with exception", e)
      case Success(_) => logger.info("Completed eservice version archiver job")
    }
    .andThen(_ => blockingThreadPool.shutdown())

  Await.ready(init, Duration.Inf)

}
