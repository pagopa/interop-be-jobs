package it.pagopa.interop.certifiedMailSender

import com.typesafe.scalalogging.Logger

import java.util.concurrent.{ExecutorService, Executors}
import scala.concurrent.ExecutionContext.global
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

object Main extends App {

  implicit val logger: Logger = Logger(this.getClass)

  logger.info("Starting sending certified mails")

  val blockingThreadPool: ExecutorService = Executors.newFixedThreadPool(Runtime.getRuntime.availableProcessors())
  implicit val blockingEC: ExecutionContextExecutor = ExecutionContext.fromExecutor(blockingThreadPool)

  val job: Future[JobExecution] = Configuration
    .read()
    .map(JobExecution.create)

  def execute(): Future[Unit] = job.flatMap(_.run().flatMap(_ => execute()))

  execute()
    .recover(ex => logger.error("There was an error while running the job", ex))
    .andThen(_ => blockingThreadPool.shutdown())(global): Unit
}
