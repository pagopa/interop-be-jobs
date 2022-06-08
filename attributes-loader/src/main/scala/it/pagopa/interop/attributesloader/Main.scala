package it.pagopa.interop.attributesloader

import akka.actor.CoordinatedShutdown
import akka.actor.typed.ActorSystem
import akka.{actor => classic}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

//shuts down the actor system in case of startup errors
case object ErrorShutdown   extends CoordinatedShutdown.Reason
case object SuccessShutdown extends CoordinatedShutdown.Reason
import cats.implicits._
object Main                 extends App with Dependencies {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  implicit val actorSystem: ActorSystem[Nothing] = ActorSystem[Nothing](Behaviors.empty, "interop-be-attributes-loader")
  implicit val executionContext: ExecutionContext      = actorSystem.executionContext
  implicit val classicActorSystem: classic.ActorSystem = actorSystem.toClassic

  logger.info("Loading attributes data...")

  val result: Future[Unit] = for {
    tokenGenerator <- interopTokenGenerator
    m2mToken       <- tokenGenerator
      .generateInternalToken(
        subject = jwtConfig.subject,
        audience = jwtConfig.audience.toList,
        tokenIssuer = jwtConfig.issuer,
        secondsDuration = jwtConfig.durationInSeconds
      )
    _ = logger.info("M2M Token obtained")
    _ <- attributeRegistryManagementService.loadCertifiedAttributes(m2mToken.serialized)
  } yield ()

  result.onComplete {
    case Success(_)  =>
      logger.info("Attributes load completed")
      CoordinatedShutdown(classicActorSystem).run(SuccessShutdown)
    case Failure(ex) =>
      logger.error("Attributes load failed", ex)
      CoordinatedShutdown(classicActorSystem).run(ErrorShutdown)
  }

}
