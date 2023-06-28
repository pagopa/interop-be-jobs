package it.pagopa.interop.attributesloader

import akka.actor.CoordinatedShutdown
import akka.actor.typed.ActorSystem
import akka.{actor => classic}
import akka.actor.typed.scaladsl.Behaviors
import it.pagopa.interop.commons.utils.TypeConversions._
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContextExecutor

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

  logger.info("Move attributes to Descriptors...")

  val result: Future[Unit] = for {
    tokenGenerator <- interopTokenGenerator(blockingEc)
    m2mToken       <- tokenGenerator
      .generateInternalToken(
        subject = jwtConfig.subject,
        audience = jwtConfig.audience.toList,
        tokenIssuer = jwtConfig.issuer,
        secondsDuration = jwtConfig.durationInSeconds
      )
    _           = logger.info("M2M Token obtained")
    cm          = catalogManagementProcessService(blockingEc)
    token       = m2mToken.serialized
    admin_token = ""
    eServicesIds <- cm.getAllEServices(admin_token).map(_.map(_.id.toString)).map(_.toList)
    _ = logger.info(s"Got all eServices: ${eServicesIds.size}")
    _ <- Future.traverseWithLatch(10)(eServicesIds)(cm.moveAttributesToDescriptors(_)(token))
    _ = logger.info(s"Done!")
  } yield ()

  result.onComplete {
    case Success(_)  =>
      logger.info("Attributes moved")
      CoordinatedShutdown(classicActorSystem).run(SuccessShutdown)
    case Failure(ex) =>
      logger.error("Attributes moving failed", ex)
      CoordinatedShutdown(classicActorSystem).run(ErrorShutdown)
  }

}
