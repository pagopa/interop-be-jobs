package it.pagopa.interop.purposesarchiver

import akka.actor.CoordinatedShutdown
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.{actor => classic}
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.purposesarchiver.system.ApplicationConfiguration
import it.pagopa.interop.purposesarchiver.service.Dependencies
import it.pagopa.interop.purposesarchiver.service.impl.QueueServiceImpl
import it.pagopa.interop.purposeprocess.client.model.PurposeVersionState._
import it.pagopa.interop.agreementprocess.events.ArchiveEvent
import it.pagopa.interop.purposeprocess.client.model.Purpose

import scala.concurrent.{ExecutionContext, Future, Await, ExecutionContextExecutor}
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}
import java.util.concurrent.{ExecutorService, Executors}

case object ErrorShutdown   extends CoordinatedShutdown.Reason
case object SuccessShutdown extends CoordinatedShutdown.Reason

object Main extends App with Dependencies {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  implicit val actorSystem: ActorSystem[Nothing] = ActorSystem[Nothing](Behaviors.empty, "interop-be-purposes-archiver")
  implicit val executionContext: ExecutionContext      = actorSystem.executionContext
  implicit val classicActorSystem: classic.ActorSystem = actorSystem.toClassic

  val blockingThreadPool: ExecutorService = Executors.newFixedThreadPool(Runtime.getRuntime.availableProcessors())
  implicit val blockingEC: ExecutionContextExecutor = ExecutionContext.fromExecutor(blockingThreadPool)

  val queueService = new QueueServiceImpl(
    ApplicationConfiguration.purposesArchiverQueueName,
    ApplicationConfiguration.visibilityTimeoutInSeconds
  )

  val ARCHIVABLE_STATES = Seq(ACTIVE, SUSPENDED, WAITING_FOR_APPROVAL, DRAFT)

  private def processVersion(purpose: Purpose): Future[Unit] =
    purpose.versions.maxByOption(_.createdAt) match {
      case Some(x) =>
        x.state match {
          case ACTIVE | SUSPENDED           => purposeProcessService.archive(purpose.id, x.id).map(_ => ())
          case DRAFT | WAITING_FOR_APPROVAL => purposeProcessService.delete(purpose.id, x.id)
          case _                            => Future.unit
        }
      case None    => Future.unit
    }

  private def execution: Future[Unit] = queueService.processMessages { message =>
    for {
      agreement <- agreementProcessService.getAgreementById(message.payload.asInstanceOf[ArchiveEvent].agreementId)
      purposes  <- purposeProcessService.getAllPurposes(agreement.eserviceId, agreement.consumerId, ARCHIVABLE_STATES)
      _         <- Future.traverse(purposes)(processVersion)
    } yield ()
  }

  logger.info("Starting purposes archiver job")

  private val init = execution
    .andThen {
      case Failure(e) => logger.info("Purposes archiver job failed with exception", e)
      case Success(_) => logger.info("Completed Purposes archiver job")
    }
    .andThen { _ =>
      actorSystem.terminate()
    }
    .transformWith(_ => actorSystem.whenTerminated)

  Await.ready(init, Duration.Inf)
}
