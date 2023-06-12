package it.pagopa.interop.tenantsattributeschecker

import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.cqrs.service.MongoDbReadModelService
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.tenantsattributeschecker.ApplicationConfiguration.{actorSystem, context, executionContext}
import it.pagopa.interop.tenantsattributeschecker.util.ReadModelQueries._
import it.pagopa.interop.tenantsattributeschecker.util.jobs.{
  applyStrategyOnExpiredAttributes,
  applyStrategyOnExpiringAttributes
}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object Main extends App {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass.getCanonicalName)

  private val readModelService: MongoDbReadModelService = new MongoDbReadModelService(
    ApplicationConfiguration.readModelConfig
  )

  logger.info("Starting tenants attributes checker job")

  private val job: Future[Unit] = for {
    tenantsExpired  <- getAllAttributesTenants(readModelService, getExpiredAttributesTenants)
    _               <- applyStrategyOnExpiredAttributes(tenantsExpired.toList)
    tenantsExpiring <- getAllAttributesTenants(readModelService, getExpiringAttributesTenants)
    _               <- applyStrategyOnExpiringAttributes(tenantsExpiring.toList)
    _ = readModelService.close()
    _ = logger.info("Completed tenants attributes checker job")
    _ = actorSystem.terminate()
    _ <- actorSystem.whenTerminated
  } yield ()

  Await.ready(job, Duration.Inf)
}
