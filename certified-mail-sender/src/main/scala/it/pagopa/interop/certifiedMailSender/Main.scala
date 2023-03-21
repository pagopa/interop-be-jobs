package it.pagopa.interop.certifiedMailSender

import com.typesafe.scalalogging.Logger
import it.pagopa.interop.commons.mail.InteropMailer
import it.pagopa.interop.commons.utils.TypeConversions.EitherOps
import it.pagopa.interop.commons.queue.impl.SQSHandler

import java.util.concurrent.{ExecutorService, Executors}
import scala.concurrent.ExecutionContext.global
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

object Main extends App {

  implicit val logger: Logger = Logger(this.getClass)

  logger.info("Starting sending certified mails")

  val blockingThreadPool: ExecutorService = Executors.newFixedThreadPool(Runtime.getRuntime.availableProcessors())
  implicit val blockingEC: ExecutionContextExecutor = ExecutionContext.fromExecutor(blockingThreadPool)

  val sqsHandler: SQSHandler    = SQSHandler(ApplicationConfiguration.queueUrl)(blockingEC)
  def job: Future[JobExecution] =
    ApplicationConfiguration.mailConfiguration
      .map(config => JobExecution(sqsHandler, InteropMailer.from(config)))
      .toFuture

  def execute(): Future[Unit] = job
    .flatMap(_.run())
    .flatMap(_ => execute())
    .recover(ex => logger.error("There was an error while running the job", ex))
    .andThen(_ => blockingThreadPool.shutdown())(global)

  execute()

}
