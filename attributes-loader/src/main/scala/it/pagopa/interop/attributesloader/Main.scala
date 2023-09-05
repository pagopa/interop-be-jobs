package it.pagopa.interop.attributesloader

import akka.actor.CoordinatedShutdown
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.{actor => classic}
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.BEARER

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

//shuts down the actor system in case of startup errors
case object ErrorShutdown   extends CoordinatedShutdown.Reason
case object SuccessShutdown extends CoordinatedShutdown.Reason

object Main extends App with Dependencies {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  implicit val actorSystem: ActorSystem[Nothing] = ActorSystem[Nothing](Behaviors.empty, "interop-be-attributes-loader")
  implicit val executionContext: ExecutionContext      = actorSystem.executionContext
  implicit val classicActorSystem: classic.ActorSystem = actorSystem.toClassic

  val blockingEc: ExecutionContextExecutor = actorSystem.dispatchers.lookup(classic.typed.DispatcherSelector.blocking())

  val jobs: Jobs =
    new Jobs(attributeRegistryProcessService(blockingEc), partyRegistryService(blockingEc), readModelService)

  logger.info("Loading attributes data...")

  def loadAttributes(): Future[Unit] = for {
    tokenGenerator <- interopTokenGenerator(blockingEc)
    m2mToken       <- tokenGenerator
      .generateInternalToken(
        subject = jwtConfig.subject,
        audience = jwtConfig.audience.toList,
        tokenIssuer = jwtConfig.issuer,
        secondsDuration = jwtConfig.durationInSeconds
      )
    _                 = logger.info("M2M Token obtained")
    contextWithBearer = contexts :+ (BEARER -> m2mToken.serialized)
    _ <- jobs.loadCertifiedAttributes()(contextWithBearer, logger, executionContext)
  } yield ()

  def run() = loadAttributes().andThen {
    case Success(_)  =>
      logger.info("Attributes load successfully completed")
      CoordinatedShutdown(classicActorSystem).run(SuccessShutdown)
    case Failure(ex) =>
      logger.error("Attributes load failed", ex)
      CoordinatedShutdown(classicActorSystem).run(ErrorShutdown)
  }

  Await.result(run(), Duration.Inf)
  logger.info("Attributes load job completed")

}
