package it.pagopa.interop.tokenreader

import it.pagopa.interop.commons.queue.QueueConfiguration
import it.pagopa.interop.commons.queue.impl.{SQSDequeuedMessage, SQSSimpleHandler}
import it.pagopa.interop.tokenreader.messages.JWTDetailsMessage
import it.pagopa.interop.tokenreader.messages.JWTDetailsMessage.jwtDetailsMessageFormat
import it.pagopa.interop.tokenreader.system.ApplicationConfiguration
import it.pagopa.interop.tokenreader.utils.FileUtils
import spray.json.enrichAny
import cats.implicits.{toFunctorOps, toTraverseOps}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

final class JobExecution(val fileUtils: FileUtils)(implicit ec: ExecutionContext) {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  val sqsHandler: SQSSimpleHandler =
    SQSSimpleHandler(QueueConfiguration.queueAccountInfo, ApplicationConfiguration.jwtQueueUrl)

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
        Future.successful(())
      } else if (list.isEmpty) {
        logger.debug("No more messages in the queue, let's persist {} messages already accumulated.", messages.size)
        processEntries(messages)
      } else if (messages.size >= ApplicationConfiguration.batchSize) {
        logger.debug(
          "More than {} messages handled, let's persist these {} messages...",
          ApplicationConfiguration.batchSize,
          messages.size
        )
        processEntries(messages.appendedAll(list)).flatMap(_ => handle(List.empty))
      } else {
        logger.debug(
          "Less than {} messages to handle, continue to get them from the queue...",
          ApplicationConfiguration.batchSize,
          messages.size
        )
        handle(messages.appendedAll(list))
      }
    }

    def recursion: Future[Unit] =
      sqsHandler
        .processMessages(ApplicationConfiguration.maxNumberOfMessages, ApplicationConfiguration.visibilityTimeout)(
          messageToString
        )(jwtDetailsMessageFormat)
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

  private def messageToString(m: JWTDetailsMessage): Future[String] = Future.successful(m.toJson.compactPrint)

  private def processEntries(entries: List[SQSDequeuedMessage[String]]) = for {
    _ <- fileUtils.writeOnFile(entries.map(_.value))
    _ <- deleteMessages(entries.map(_.receiptHandle))
  } yield ()

  private def deleteMessages(receiptHandles: List[String]): Future[Unit] =
    receiptHandles.traverse(handle => sqsHandler.deleteMessage(handle)).void
}
