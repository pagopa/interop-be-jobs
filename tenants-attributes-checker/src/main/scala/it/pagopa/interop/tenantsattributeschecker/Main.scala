package it.pagopa.interop.tenantsattributeschecker

import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.cqrs.service.MongoDbReadModelService
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.tenantsattributeschecker.ApplicationConfiguration.{actorSystem, context, executionContext}
import it.pagopa.interop.tenantsattributeschecker.util.ReadModelQueries._
import it.pagopa.interop.tenantsattributeschecker.util.jobs.applyStrategy

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main extends App {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass.getCanonicalName)

  val readModelService: MongoDbReadModelService = new MongoDbReadModelService(ApplicationConfiguration.readModelConfig)

  logger.info("Starting tenants attributes checker job")

  val job = for {
    tenants <- getAllExpiredAttributesTenants(readModelService)
//    tenantsData = tenants
//      .groupBy(_.id)
//      .map { case (id, tenantValues) =>
//        TenantData(id, tenantValues.map(_.attributes))
//      }
//      .toList
    _ = applyStrategy(tenants)
    // TODO SEND PECs
    _ = readModelService.close()
    _ = logger.info("Completed tenants attributes checker job")
    _ = actorSystem.terminate()
    _ <- actorSystem.whenTerminated

  } yield ()

  Await.ready(job, Duration.Inf)

}
