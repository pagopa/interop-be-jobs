package it.pagopa.interop.tokendetailspersister.tokendetailspersister

import cats.implicits._
import it.pagopa.interop.commons.queue.impl.SQSHandler
import com.typesafe.scalalogging.Logger
import spray.json.BasicFormats

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContextExecutor

final case class JobExecution(fileUtils: FileUtils)(blockingExecutionContext: ExecutionContextExecutor)
    extends BasicFormats {

  private implicit val ec: ExecutionContext = blockingExecutionContext
  private val logger: Logger                = Logger(this.getClass)
  private val sqsHandler: SQSHandler        = SQSHandler(ApplicationConfiguration.jwtQueueUrl)(blockingExecutionContext)

  def run(): Future[Unit] = sqsHandler.processAllRawMessages(
    ApplicationConfiguration.maxNumberOfMessagesPerFile,
    ApplicationConfiguration.visibilityTimeout
  ) { (messages: List[String], receipts: List[String]) =>
    logger.info(s"Saving ${messages.size} messages")
    fileUtils.store(messages) >> deleteMessages(receipts)
  }

  private def deleteMessages(receiptHandles: List[String]): Future[Unit] =
    Future.traverse(receiptHandles)(sqsHandler.deleteMessage).void
}
