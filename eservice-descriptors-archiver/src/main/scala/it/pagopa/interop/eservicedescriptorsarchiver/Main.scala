package it.pagopa.interop.eservicedescriptorsarchiver

import scala.concurrent.Future

object Main extends App with Dependencies {

  final val chunkSize: Int = 10

  logger.info("Starting eservice versions archiver job")

  private val execution: Future[Unit] = sqsHandler.processAllRawMessages(chunkSize)(processMessages)

  private def executionLoop(): Future[Unit] = execution.flatMap(_ => executionLoop())

  executionLoop()
    .recover(ex => logger.error("There was an error while running the job", ex))
    .andThen(_ => blockingThreadPool.shutdown())(ec): Unit

}
