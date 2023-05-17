package it.pagopa.interop.tenantsattributeschecker

import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.cqrs.service.{MongoDbReadModelService, ReadModelService}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.tenantsattributeschecker.ApplicationConfiguration.{actorSystem, context, executionContext}
import it.pagopa.interop.tenantsattributeschecker.util.ReadModelQueries._
import it.pagopa.interop.tenantsattributeschecker.util.jobs.applyStrategy
import it.pagopa.interop.tenantsattributeschecker.util.{ExpiringAgreements, TenantData}
//import it.pagopa.interop.tenantsattributeschecker.service.AgreementProcessService

import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main extends App {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass.getCanonicalName)

  val readModelService: ReadModelService = new MongoDbReadModelService(ApplicationConfiguration.readModelConfig)

  logger.info("Starting tenants attributes checker job")

  val job = for {
    tenants <- getAllExpiredAttributesTenants(readModelService)
    _                                  = tenants.map(println(_))
    tenantsData: Map[UUID, TenantData] = tenants
      .groupBy(_.id)
      .view
      .mapValues(tenantValues =>
        TenantData(
          tenantValues.map(_.attributes),
          tenantValues.head.mails // getting the mails from the head because they are the same in every record
        )
      )
      .toMap

    _                 = tenantsData.map(println(_))
    expiredAttributes = applyStrategy(tenantsData)
    agreements <- getExpiredAttributesAgreements(readModelService, expiredAttributes)
    _                  = agreements.map(println(_))
    expiringAgreements = agreements.map(agreement =>
      ExpiringAgreements(
        agreement.id,
        agreement.eserviceId,
        agreement.descriptorId,
        agreement.consumerId, {
          val tenantData          = tenantsData(agreement.consumerId)
          val agreementAttributes = agreement.verifiedAttributes.map(_.id)
          TenantData(
            tenantData.attributesExpired.filter(tenantAttribute => agreementAttributes.contains(tenantAttribute.id)),
            tenantData.mails
          )
        }
      )
    )
    _                  = expiringAgreements.map(println(_))
    _                  = readModelService.close()
    _                  = logger.info("Completed tenants attributes checker job")
    _                  = actorSystem.terminate()
    _ <- actorSystem.whenTerminated

  } yield ()

  Await.ready(job, Duration.Inf)

}
