package it.pagopa.interop.tenantscertifiedattributesupdater.service.impl

import cats.implicits._
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.tenantprocess.client.api.TenantApi
import it.pagopa.interop.tenantprocess.client.invoker.{ApiRequest, BearerToken}
import it.pagopa.interop.tenantprocess.client.model.{InternalTenantSeed, Tenant}
import it.pagopa.interop.tenantscertifiedattributesupdater.service.{TenantProcessInvoker, TenantProcessService}

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
}
