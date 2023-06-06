package it.pagopa.interop.tenantsattributeschecker

import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.cqrs.service.MongoDbReadModelService
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.tenantmanagement.model.tenant.PersistentVerifiedAttribute
import it.pagopa.interop.tenantsattributeschecker.ApplicationConfiguration.{actorSystem, context, executionContext}
import it.pagopa.interop.tenantsattributeschecker.service.impl.MailTemplate
import it.pagopa.interop.tenantsattributeschecker.util.ReadModelQueries.getAllExpiredAttributesTenants
import it.pagopa.interop.tenantsattributeschecker.util.jobs.{applyStrategy, sendEnvelope}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

object Main extends App {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass.getCanonicalName)

  private val readModelService: MongoDbReadModelService = new MongoDbReadModelService(
    ApplicationConfiguration.readModelConfig
  )
  private val expirationMailTemplate: MailTemplate      = MailTemplate.expiration()

  logger.info("Starting tenants attributes checker job")

  private val job: Future[Unit] = for {
    tenants        <- getAllExpiredAttributesTenants(readModelService)
    _              <- applyStrategy(tenants.toList)
    tenantsInMonth <- getAllExpiredAttributesTenants(readModelService, expirationInMonth = true)
    _              <- Future.traverse(tenantsInMonth) { tenant =>
      Future.traverse(tenant.attributes.collect { case v: PersistentVerifiedAttribute => v }) { attribute =>
        Future.traverse(attribute.verifiedBy) { verifiedBy =>
          sendEnvelope(attribute.id, tenant, verifiedBy, expirationMailTemplate)
        }
      }
    }
    _ = readModelService.close()
    _ = logger.info("Completed tenants attributes checker job")
    _ = actorSystem.terminate()
    _ <- actorSystem.whenTerminated
  } yield ()

  Await.ready(job, Duration.Inf)
}
