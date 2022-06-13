package it.pagopa.interop.tokenreader

import cats.implicits.toFunctorOps
import it.pagopa.interop.commons.queue.impl.{SQSDequeuedMessage, SQSSimpleHandler}
import it.pagopa.interop.tokenreader.system.ApplicationConfiguration
import it.pagopa.interop.tokenreader.utils.FileUtils
import org.slf4j.{Logger, LoggerFactory}
import spray.json.BasicFormats

import scala.concurrent.{ExecutionContext, Future}

final case class JobExecution(fileUtils: FileUtils)(implicit ec: ExecutionContext) extends BasicFormats {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  val sqsHandler: SQSSimpleHandler = SQSSimpleHandler(ApplicationConfiguration.jwtQueueUrl)

  /**
   * Performs a series of iterations until the Queue of tokens is empty
   * @param messages
   * @return
   */
  def run(): Future[Unit] = handle(List.empty)

  private def handle(messages: List[SQSDequeuedMessage[String]]): Future[Unit] = {
    def process(list: List[SQSDequeuedMessage[String]]): Future[Unit] = {
      logger.debug(
        "Processing {} messages from the queue. So far, {} messages have been accumulated",
        list.size,
        messages.size
      )
      if (list.isEmpty && messages.isEmpty) {
        logger.info("No more messages to process, Howdy ho!")
        Future.unit
      } else if (list.isEmpty) {
        logger.debug("No more messages in the queue, let's persist {} messages already accumulated.", messages.size)
        processEntries(messages)
      } else if (messages.size >= ApplicationConfiguration.maxNumberOfMessagesPerFile) {
        logger.debug(
          "More than {} messages handled, let's persist these {} messages...",
          ApplicationConfiguration.maxNumberOfMessagesPerFile,
          messages.size
        )
        processEntries(messages.appendedAll(list)).flatMap(_ => handle(List.empty))
      } else {
        logger.debug(
          "Less than {} messages to handle, continue to get them from the queue...",
          ApplicationConfiguration.maxNumberOfMessagesPerFile,
          messages.size
        )
        handle(messages.appendedAll(list))
      }
    }

    def recursion: Future[Unit] =
      sqsHandler
        .processMessages[String, String](
          ApplicationConfiguration.batchSize,
          ApplicationConfiguration.visibilityTimeout
        )(Future.successful)
        .flatMap(list => process(list))
        .recoverWith { case ex =>
          logger.error(
            "Something went wrong while processing messages. Let's go ahead with the next bunch of messages in the queue...",
            ex
          )
          handle(messages)
        }

    recursion
  }

  private def processEntries(entries: List[SQSDequeuedMessage[String]]) = for {
    _ <- fileUtils.writeOnFile(entries.map(_.value))
    _ <- deleteMessages(entries.map(_.receiptHandle))
  } yield ()

  private def deleteMessages(receiptHandles: List[String]): Future[Unit] =
    Future.traverse(receiptHandles)(handle => sqsHandler.deleteMessage(handle)).void
}
