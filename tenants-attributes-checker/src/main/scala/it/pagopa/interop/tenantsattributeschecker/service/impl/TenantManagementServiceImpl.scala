//package it.pagopa.interop.tenantsattributeschecker.service.impl
//
//import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
//import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
//import it.pagopa.interop.commons.utils.withHeaders
//import it.pagopa.interop.tenantmanagement.client.api.AttributesApi
//import it.pagopa.interop.tenantmanagement.client.invoker.{ApiInvoker, BearerToken}
//import it.pagopa.interop.tenantmanagement.client.model.{Tenant, TenantAttribute}
//import it.pagopa.interop.tenantsattributeschecker.ApplicationConfiguration
//import it.pagopa.interop.tenantsattributeschecker.ApplicationConfiguration._
//import it.pagopa.interop.tenantsattributeschecker.service.TenantManagementService
//
//import java.util.UUID
//import scala.concurrent.{ExecutionContextExecutor, Future}
//
//final case class TenantManagementServiceImpl(blockingEc: ExecutionContextExecutor) extends TenantManagementService {
//
//  private val invoker: ApiInvoker = ApiInvoker(blockingEc)
//  val api: AttributesApi          = AttributesApi(ApplicationConfiguration.tenantManagementURL)
//
//  private implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
//    Logger.takingImplicit[ContextFieldsToLog](this.getClass)
//
//  override def updateTenantAttribute(tenantId: UUID, attributeId: UUID, attribute: TenantAttribute)(implicit
//    contexts: Seq[(String, String)]
//  ): Future[Tenant] =
//    withHeaders { (bearerToken, correlationId, ip) =>
//      val request = api.updateTenantAttribute(
//        xCorrelationId = correlationId,
//        tenantId = tenantId,
//        attributeId = attributeId,
//        tenantAttribute = attribute,
//        xForwardedFor = ip
//      )(BearerToken(bearerToken))
//      invoker.invoke(request, s"Updating attribute $attributeId from tenant $tenantId")
//    }
//}
