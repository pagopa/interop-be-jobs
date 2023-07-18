package it.pagopa.interop.purposesarchiver

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.{actor => classic}
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.BEARER
import it.pagopa.interop.purposesarchiver.system.ApplicationConfiguration
import it.pagopa.interop.purposesarchiver.service.impl.QueueServiceImpl
import it.pagopa.interop.purposesarchiver.service.{Dependencies, QueueService}

import scala.concurrent.{ExecutionContext, Future, ExecutionContextExecutor}
import java.util.concurrent.{ExecutorService, Executors}

object Main extends App with Dependencies {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  implicit val actorSystem: ActorSystem[Nothing] = ActorSystem[Nothing](Behaviors.empty, "interop-be-purposes-archiver")
  implicit val executionContext: ExecutionContext      = actorSystem.executionContext
  implicit val classicActorSystem: classic.ActorSystem = actorSystem.toClassic

  val blockingThreadPool: ExecutorService = Executors.newFixedThreadPool(Runtime.getRuntime.availableProcessors())
  implicit val blockingEC: ExecutionContextExecutor = ExecutionContext.fromExecutor(blockingThreadPool)

  val queueService: QueueService = new QueueServiceImpl(
    ApplicationConfiguration.purposesArchiverQueueName,
    ApplicationConfiguration.visibilityTimeoutInSeconds
  )

  private def run: Future[Unit] = for {
    bearer <- generateBearer
    _                 = logger.info("Internal Token obtained")
    contextWithBearer = contexts :+ (BEARER -> bearer)
    _ <- queueService.receive()
  } yield ()

  def execute(): Future[Unit] = run.flatMap(_ => execute())

  logger.info("Starting purposes archiver job")

  execute()
    .recover(ex => logger.error("There was an error while running the job", ex))
    .andThen(_ => blockingThreadPool.shutdown()): Unit
}
