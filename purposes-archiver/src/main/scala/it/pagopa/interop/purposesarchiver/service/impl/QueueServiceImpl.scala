package it.pagopa.interop.purposesarchiver.service.impl

import akka.actor.typed.ActorSystem
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import io.circe.generic.auto._
import io.circe.jawn.parse
import software.amazon.awssdk.services.sqs.model.Message
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.queue.impl.SQSHandler
import it.pagopa.interop.commons.queue.config.SQSHandlerConfig
import it.pagopa.interop.commons.utils.TypeConversions.EitherOps
import it.pagopa.interop.purposesarchiver.service.{AgreementProcessInvoker, PurposeInvoker}
import it.pagopa.interop.purposesarchiver.service.impl.{AgreementProcessServiceImpl, PurposeProcessServiceImpl}
import it.pagopa.interop.purposesarchiver.system.ApplicationConfiguration
import it.pagopa.interop.purposesarchiver.service.QueueService
import it.pagopa.interop.agreementprocess.client.api.AgreementApi
import it.pagopa.interop.agreementprocess.events.ArchiveEvent
import it.pagopa.interop.purposeprocess.client.api.PurposeApi
import it.pagopa.interop.purposeprocess.client.model.PurposeVersionState._
import it.pagopa.interop.purposeprocess.client.model.Purpose

import scala.concurrent.{ExecutionContextExecutor, Future}

final class QueueServiceImpl(queueName: String, visibilityTimeoutInSeconds: Int)(implicit
  blockingEc: ExecutionContextExecutor,
  actorSystem: ActorSystem[_]
) extends QueueService {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  val agreementProcessService: AgreementProcessServiceImpl =
    AgreementProcessServiceImpl(
      AgreementProcessInvoker(blockingEc)(actorSystem.classicSystem),
      AgreementApi(ApplicationConfiguration.agreementProcessURL)
    )

  val purposeProcessService: PurposeProcessServiceImpl =
    PurposeProcessServiceImpl(
      PurposeInvoker(blockingEc)(actorSystem.classicSystem),
      PurposeApi(ApplicationConfiguration.purposeProcessURL)
    )

  val ARCHIVABLE_STATES = Seq(ACTIVE, SUSPENDED, WAITING_FOR_APPROVAL, DRAFT)

  val sqsHandlerConfig: SQSHandlerConfig =
    SQSHandlerConfig(queueUrl = queueName, visibilityTimeout = visibilityTimeoutInSeconds)

  val sqsHandler: SQSHandler = new SQSHandler(sqsHandlerConfig)(blockingEc)

  override def receive()(implicit contexts: Seq[(String, String)]): Future[Unit] = sqsHandler
    .rawReceive()
    .flatMap {
      case None          => Future.unit
      case Some(message) => processMessage(message)
    }

  private def processVersion(purpose: Purpose)(implicit contexts: Seq[(String, String)]): Future[Unit] =
    purpose.versions.maxByOption(_.createdAt).fold(Future.unit) { v =>
      v.state match {
        case ACTIVE | SUSPENDED           => purposeProcessService.archive(purpose.id, v.id).map(_ => ())
        case DRAFT | WAITING_FOR_APPROVAL => purposeProcessService.delete(purpose.id, v.id)
        case _                            => Future.unit
      }
    }

  private def deleteMessage(event: ArchiveEvent, receipt: String)(implicit
    contexts: Seq[(String, String)]
  ): Future[Unit] =
    sqsHandler
      .deleteMessage(receipt)
      .map(_ => logger.info(s"Message deleted from queue"))
      .recoverWith { ex =>
        logger.error(
          s"Error trying to delete archive event for agreement ${event.agreementId.toString} with receipt $receipt - ${ex.getMessage}"
        )
        Future.failed(ex)
      }

  private def processMessage(message: Message)(implicit contexts: Seq[(String, String)]): Future[Unit] = for {
    event     <- parse(message.body()).flatMap(_.as[ArchiveEvent]).toFuture
    agreement <- agreementProcessService.getAgreementById(event.agreementId)
    purposes  <- purposeProcessService.getAllPurposes(agreement.eserviceId, agreement.consumerId, ARCHIVABLE_STATES)
    _         <- Future.traverse(purposes)(processVersion)
    _ = logger.info(
      s"Deleting archive event for agreement ${event.agreementId.toString} with receipt ${message.receiptHandle()}"
    )
    _ <- deleteMessage(event, message.receiptHandle())
  } yield ()
}
