package it.pagopa.interop.certifiedMailSender

import cats.implicits.toTraverseOps
import com.typesafe.scalalogging.Logger
import courier._
import io.circe.generic.auto._
import io.circe.jawn.parse
import it.pagopa.interop.certifiedMailSender.model.InteropEnvelope
import it.pagopa.interop.commons.queue.impl.SQSHandler
import it.pagopa.interop.commons.utils.TypeConversions.EitherOps
import software.amazon.awssdk.services.sqs.model.Message

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.Try

final case class JobExecution(sqsHandler: SQSHandler) {

  private val logger: Logger = Logger(this.getClass)

  implicit val mailer: Mailer = Mailer(ApplicationConfiguration.serverAddress, ApplicationConfiguration.serverPort)
    .auth(true)
    .as(ApplicationConfiguration.smtpUser, ApplicationConfiguration.smtpPassword)
    .ssl(true)()

  def run()(implicit blockingEC: ExecutionContextExecutor): Future[Unit] =
    sqsHandler.rawReceive(ApplicationConfiguration.visibilityTimeout).flatMap {
      case None          => Future.unit
      case Some(message) => processMessage(message)
    }

  private def processMessage(message: Message)(implicit ec: ExecutionContext): Future[Unit] =
    sendMail(message.body()).flatMap(deleteMessage(message.receiptHandle()))

  private def deleteMessage(receipt: String)(implicit ec: ExecutionContext): InteropEnvelope => Future[Unit] =
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

  private def sendMail(message: String)(implicit mailer: Mailer, ec: ExecutionContext): Future[InteropEnvelope] = for {
    interopEnvelope <- parse(message).flatMap(_.as[InteropEnvelope]).toFuture
    _ = logger.info(s"Sending envelope ${interopEnvelope.id.toString}")
    _ <- sendMail(interopEnvelope)
    _ = logger.info(s"Envelope ${interopEnvelope.id.toString} sent")
  } yield interopEnvelope

  private def sendMail(interopEnvelop: InteropEnvelope)(implicit mailer: Mailer, ec: ExecutionContext): Future[Unit] =
    prepareEnvelope(interopEnvelop).toFuture
      .flatMap(mailer(_))
      .recoverWith { ex =>
        logger.error(s"Error trying to send envelope ${interopEnvelop.id.toString} - ${ex.getMessage}")
        Future.failed(ex)
      }

  private def prepareEnvelope(envelop: InteropEnvelope): Either[Throwable, Envelope] = {
    val content: Content = envelop.attachments
      .foldLeft(Multipart().html(envelop.body))((mailContent, attachment) =>
        mailContent.attachBytes(attachment.bytes, attachment.name, attachment.mimetype)
      )
    for {
      to  <- envelop.to.traverse(t => Try(t.addr))
      cc  <- envelop.cc.traverse(c => Try(c.addr))
      bcc <- envelop.bcc.traverse(b => Try(b.addr))
    } yield Envelope
      .from(ApplicationConfiguration.senderAddress.addr)
      .to(to: _*)
      .cc(cc: _*)
      .bcc(bcc: _*)
      .subject(envelop.subject)
      .content(content)

  }.toEither
}
