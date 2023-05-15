package it.pagopa.interop.tenantsattributeschecker

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.{actor => classic}
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.cqrs.service.{MongoDbReadModelService, ReadModelService}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.CORRELATION_ID_HEADER
import it.pagopa.interop.tenantsattributeschecker.util.ReadModelQueries.{
  getExpiredAttributesAgreements,
  getExpiredAttributesTenants
}
//import it.pagopa.interop.tenantsattributeschecker.service.AgreementProcessService

import java.util.UUID
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor}

object Main extends App with Dependencies {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass.getCanonicalName)
  implicit val actorSystem: ActorSystem[Nothing]                =
    ActorSystem[Nothing](Behaviors.empty, "interop-be-tenants-certified-attributes-updater")
  implicit val executionContext: ExecutionContext               = actorSystem.executionContext
  implicit val classicActorSystem: classic.ActorSystem          = actorSystem.toClassic

  val blockingEc: ExecutionContextExecutor = actorSystem.dispatchers.lookup(classic.typed.DispatcherSelector.blocking())
  implicit val context: List[(String, String)]    = (CORRELATION_ID_HEADER -> UUID.randomUUID().toString) :: Nil
  implicit val readModelService: ReadModelService = new MongoDbReadModelService(
    ApplicationConfiguration.readModelConfig
  )

//  val agreementsProcessService: AgreementProcessService = agreementProcessService(blockingEc)

  logger.info("Starting tenants attributes checker job")

//  def resources(implicit
//                ec: ExecutionContext
//               ): Future[(ExecutorService, ExecutionContextExecutor, ReadModelService, Jobs, Configuration)] = for {
//    config <- Configuration.read()
//    es <- Future(Executors.newFixedThreadPool(1.max(Runtime.getRuntime.availableProcessors() - 1)))
//    blockingEC = ExecutionContext.fromExecutor(es)
//    fm <- Future(FileManager.get(FileManager.S3)(blockingEC))
//    rm <- Future(new MongoDbReadModelService(config.readModel))
//    jobs <- Future(new Jobs(config, fm, rm))
//  } yield (es, blockingEC, fm, rm, jobs, config)

  val job = for {
    tenants <- getExpiredAttributesTenants(readModelService)
    expiredAttributes = tenants.flatMap(_.attributes.map(_.id.toString))
    agreements <- getExpiredAttributesAgreements(readModelService, expiredAttributes)
    _ = readModelService.close()
    _ = logger.info("Completed tenants attributes checker job")
    _ = actorSystem.terminate()
    _ <- actorSystem.whenTerminated

  } yield ()

  Await.ready(job, Duration.Inf)

}
