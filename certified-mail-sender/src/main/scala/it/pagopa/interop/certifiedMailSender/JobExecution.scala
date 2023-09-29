package it.pagopa.interop.certifiedMailSender

import it.pagopa.interop.commons.mail.Mail._
import com.typesafe.scalalogging.Logger
import io.circe.jawn.parse
import it.pagopa.interop.commons.mail.{InteropMailer, TextMail}
import it.pagopa.interop.commons.queue.config.SQSHandlerConfig
import it.pagopa.interop.commons.queue.impl.SQSHandler
import it.pagopa.interop.commons.utils.TypeConversions.EitherOps
import software.amazon.awssdk.services.sqs.model.Message

import scala.concurrent.{ExecutionContextExecutor, Future}

final class JobExecution private (config: Configuration)(implicit blockingEC: ExecutionContextExecutor) {

  private val logger: Logger = Logger(this.getClass)

  private val sqsHandlerConfig: SQSHandlerConfig =
    SQSHandlerConfig(queueUrl = config.queue.url, visibilityTimeout = config.queue.visibilityTimeoutInSeconds)
  private val sqsHandler: SQSHandler             = SQSHandler(sqsHandlerConfig)(blockingEC)
  private val mailer: InteropMailer              = InteropMailer.from(config.mail)

  def run(): Future[Unit] = sqsHandler
    .rawReceive()
    .flatMap {
      case None          => Future.unit
      case Some(message) => processMessage(message)
    }

  private def processMessage(message: Message): Future[Unit] =
    sendMail(message.body()).flatMap(deleteMessage(message.receiptHandle()))

  private def deleteMessage(receipt: String): TextMail => Future[Unit] =
    envelope => {
      logger.info(s"Deleting envelope ${envelope.id.toString} with receipt $receipt")
      sqsHandler
        .deleteMessage(receipt)
        .map(_ => logger.info(s"Message deleted from queue"))
        .recoverWith { ex =>
          logger.error(
            s"Error trying to delete envelope ${envelope.id.toString} with receipt $receipt - ${ex.getMessage}"
          )
          Future.failed(ex)
        }
    }

  private def sendMail(message: String): Future[TextMail] =
    for {
      mail <- parse(message).flatMap(_.as[TextMail]).toFuture
      _ = logger.info(s"Sending envelope ${mail.id.toString}")
      _ <- sendMail(mail)
      _ = logger.info(s"Envelope ${mail.id.toString} sent")
    } yield mail

  private def sendMail(mail: TextMail): Future[Unit] =
    mailer
      .send(mail)
      .recoverWith { ex =>
        logger.error(s"Error trying to send envelope ${mail.id.toString} - ${ex.getMessage}")
        Future.failed(ex)
      }
}

object JobExecution {
  def create(config: Configuration)(implicit blockingEC: ExecutionContextExecutor) = new JobExecution(config)
}
