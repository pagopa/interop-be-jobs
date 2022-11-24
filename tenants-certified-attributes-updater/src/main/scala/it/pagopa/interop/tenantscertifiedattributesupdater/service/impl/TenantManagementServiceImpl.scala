package it.pagopa.interop.tenantscertifiedattributesupdater.service.impl

import cats.implicits._
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.tenantmanagement.client.api.TenantApi
import it.pagopa.interop.tenantmanagement.client.invoker.{ApiRequest, BearerToken}
import it.pagopa.interop.tenantmanagement.client.model._
import it.pagopa.interop.tenantscertifiedattributesupdater.service.{TenantManagementInvoker, TenantManagementService}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

final case class TenantManagementServiceImpl(invoker: TenantManagementInvoker, api: TenantApi)(implicit
  ec: ExecutionContext
) extends TenantManagementService {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  override def updateTenant(
    bearerToken: String
  )(tenantId: UUID, delta: TenantDelta)(implicit contexts: Seq[(String, String)]): Future[Unit] = {
    val request: ApiRequest[Tenant] =
      api.updateTenant(xCorrelationId = UUID.randomUUID().toString, tenantId = tenantId, tenantDelta = delta)(
        BearerToken(bearerToken)
      )

    invoker
      .invoke(request, s"Updating tenant $tenantId")
      .void
      .recoverWith { case _ =>
        logger.error(s"Fail updating tenant $tenantId")
        Future.unit
      }
  }
}
