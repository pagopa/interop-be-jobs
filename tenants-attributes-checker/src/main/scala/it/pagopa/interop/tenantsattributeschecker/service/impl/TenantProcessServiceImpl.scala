package it.pagopa.interop.tenantsattributeschecker.service.impl

import akka.actor.typed.ActorSystem
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.withHeaders
import it.pagopa.interop.tenantprocess.client.api.{EnumsSerializers, TenantApi}
import it.pagopa.interop.tenantprocess.client.invoker.{ApiInvoker, ApiRequest, BearerToken}
import it.pagopa.interop.tenantprocess.client.model.Tenant
import it.pagopa.interop.tenantsattributeschecker.ApplicationConfiguration
import it.pagopa.interop.tenantsattributeschecker.service.TenantProcessService

import java.util.UUID
import scala.concurrent.{ExecutionContextExecutor, Future}

final case class TenantProcessServiceImpl(blockingEc: ExecutionContextExecutor)(implicit system: ActorSystem[_])
    extends TenantProcessService {

  private val invoker: ApiInvoker = ApiInvoker(EnumsSerializers.all, blockingEc)(system.classicSystem)
  private val api: TenantApi      = TenantApi(ApplicationConfiguration.tenantProcessURL)

  private implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  override def updateVerifiedAttributeExtensionDate(tenantId: UUID, attributeId: UUID, verifierId: UUID)(implicit
    contexts: Seq[(String, String)]
  ): Future[Tenant] =
    withHeaders[Tenant] { (bearerToken, correlationId) =>
      val request: ApiRequest[Tenant] =
        api.updateVerifiedAttributeExtensionDate(
          xCorrelationId = correlationId,
          tenantId = tenantId,
          attributeId = attributeId,
          verifierId = verifierId
        )(BearerToken(bearerToken))
      invoker.invoke(request, s"Updating verified attribute $attributeId to $tenantId")
    }

  override def getTenant(tenantId: UUID)(implicit contexts: Seq[(String, String)]): Future[Tenant] =
    withHeaders[Tenant] { (bearerToken, correlationId) =>
      val request: ApiRequest[Tenant] =
        api.getTenant(xCorrelationId = correlationId, id = tenantId)(BearerToken(bearerToken))
      invoker.invoke(request, s"Getting tenant with id $tenantId")
    }
}
