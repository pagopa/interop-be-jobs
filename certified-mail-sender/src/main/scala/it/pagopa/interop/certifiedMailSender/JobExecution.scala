package it.pagopa.interop.certifiedMailSender

import cats.implicits.{catsSyntaxFlatMapOps, toFunctorOps, toTraverseOps}
import com.typesafe.scalalogging.Logger
import courier._
import io.circe.generic.auto._
import io.circe.jawn.parse
import it.pagopa.interop.certifiedMailSender.model.InteropEnvelope
import it.pagopa.interop.commons.queue.impl.SQSHandler
import it.pagopa.interop.commons.utils.TypeConversions.EitherOps

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.Try

final case class JobExecution(sqsHandler: SQSHandler) {

  private val logger: Logger = Logger(this.getClass)

  implicit val mailer: Mailer = Mailer(ApplicationConfiguration.serverAddress, ApplicationConfiguration.serverPort)
    .auth(true)
    .as(ApplicationConfiguration.smtpUser, ApplicationConfiguration.smtpPassword)
    .ssl(true)()

  def run()(implicit blockingEC: ExecutionContextExecutor): Future[Unit] = sqsHandler.processAllRawMessages(
    ApplicationConfiguration.maxNumberOfMessages,
    ApplicationConfiguration.visibilityTimeout
  ) { (messages, receipts) =>
    logger.info(s"Sending ${messages.size} messages")
    sendMessages(messages) >> deleteMessages(receipts)
  }

  private def sendMessages(messages: List[String])(implicit ec: ExecutionContext): Future[Unit] =
    Future.traverse(messages)(sendMail).void

  private def deleteMessages(receiptHandles: List[String])(implicit ec: ExecutionContext): Future[Unit] =
    Future.traverse(receiptHandles)(sqsHandler.deleteMessage).void

  private def sendMail(message: String)(implicit ec: ExecutionContext): Future[Unit] = for {
    interopEnvelop <- parse(message).flatMap(_.as[InteropEnvelope]).toFuture
    _ = logger.info(s"Sending envelope ${interopEnvelop.id.toString}")
    result <- sendMail(interopEnvelop)
    _ = logger.info(s"Envelope ${interopEnvelop.id.toString} sent")
    _ = logger.info(s"Deleting envelope ${interopEnvelop.id.toString}")
    _ = logger.info(s"Envelope ${interopEnvelop.id.toString} deleted")
  } yield result

  private def sendMail(interopEnvelop: InteropEnvelope)(implicit mailer: Mailer, ec: ExecutionContext): Future[Unit] =
    for {
      envelope <- prepareEnvelope(interopEnvelop).toFuture
      result   <- mailer(envelope)
    } yield result

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
