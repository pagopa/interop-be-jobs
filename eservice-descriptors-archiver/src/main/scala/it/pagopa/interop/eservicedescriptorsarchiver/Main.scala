package it.pagopa.interop.eservicedescriptorsarchiver

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object Main extends App with Dependencies {

  final val chunkSize: Int = 10

  logger.info("Starting eservice versions archiver job")

  def run(): Future[Unit] = sqsHandler
    .processAllRawMessages(chunkSize)(processMessages)
    .flatMap(_ => run())
    .recoverWith { e =>
      logger.error("Eservice version archiver job failed with exception", e)
      run()
    }

  Await.result(run(), Duration.Inf)

}
