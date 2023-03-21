package it.pagopa.interop.tenantscertifiedattributesupdater.service.impl

import cats.implicits._
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.tenantprocess.client.api.TenantApi
import it.pagopa.interop.tenantprocess.client.invoker.{ApiRequest, BearerToken}
import it.pagopa.interop.tenantprocess.client.model.{InternalTenantSeed, Tenant}
import it.pagopa.interop.tenantscertifiedattributesupdater.service.{TenantProcessInvoker, TenantProcessService}
import it.pagopa.interop.tenantmanagement.model.tenant.PersistentExternalId
import it.pagopa.interop.tenantscertifiedattributesupdater.util.AttributeInfo

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

final case class TenantProcessServiceImpl(invoker: TenantProcessInvoker, api: TenantApi)(implicit ec: ExecutionContext)
    extends TenantProcessService {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  override def upsertTenant(
    bearerToken: String
  )(seed: InternalTenantSeed)(implicit contexts: Seq[(String, String)]): Future[Unit] = {
    val request: ApiRequest[Tenant] =
      api.internalUpsertTenant(xCorrelationId = UUID.randomUUID().toString, internalTenantSeed = seed)(
        BearerToken(bearerToken)
      )

    invoker
      .invoke(request, s"Upserting tenant ${seed.externalId.origin}/${seed.externalId.value}")
      .void
      .recoverWith { case _ =>
        logger.error(s"Fail upserting seed ${seed.toString}")
        Future.unit
      }
  }

  override def revokeCertifiedAttribute(
    bearerToken: String
  )(externalId: PersistentExternalId, attributeInfo: AttributeInfo)(implicit
    contexts: Seq[(String, String)]
  ): Future[Unit] = {
    val request: ApiRequest[Unit] =
      api.internalRevokeCertifiedAttribute(
        xCorrelationId = UUID.randomUUID().toString,
        tOrigin = externalId.origin,
        tExternalId = externalId.value,
        aOrigin = attributeInfo.origin,
        aExternalId = attributeInfo.code
      )(BearerToken(bearerToken))

    invoker
      .invoke(
        request,
        s"Revoke certified attribute ${attributeInfo.origin}/${attributeInfo.code} for tenant ${externalId.origin}/${externalId.value}"
      )
      .recoverWith { case _ =>
        logger.error(
          s"Fail revoking certified attribute ${attributeInfo.origin}/${attributeInfo.code} for tenant ${externalId.origin}/${externalId.value}"
        )
        Future.unit
      }
  }
}
