//package it.pagopa.interop.tenantsattributeschecker.service.impl
//
//import akka.actor.typed.ActorSystem
//import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
//import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
//import it.pagopa.interop.commons.utils.withHeaders
//import it.pagopa.interop.tenantprocess.client.api.{EnumsSerializers, TenantApi}
//import it.pagopa.interop.tenantprocess.client.invoker.{ApiInvoker, BearerToken}
//import it.pagopa.interop.tenantprocess.client.model.Tenant
//import it.pagopa.interop.tenantsattributeschecker.service.TenantProcessService
//
//import java.util.UUID
//import scala.concurrent.{ExecutionContextExecutor, Future}
//
//final case class TenantProcessServiceImpl(tenantprocessUrl: String, blockingEc: ExecutionContextExecutor)(implicit
//  system: ActorSystem[_]
//) extends TenantProcessService {
//
//  private val invoker: ApiInvoker = ApiInvoker(EnumsSerializers.all, blockingEc)(system.classicSystem)
//  val api: TenantApi              = TenantApi(tenantprocessUrl)
//
//  private implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
//    Logger.takingImplicit[ContextFieldsToLog](this.getClass)
//
//  override def revokeVerifiedAttribute(tenantId: UUID, attributeId: UUID)(implicit
//    contexts: Seq[(String, String)]
//  ): Future[Tenant] = withHeaders[Tenant] { (bearerToken, correlationId, ip) =>
//    val request =
//      api.revokeVerifiedAttribute(
//        xCorrelationId = correlationId,
//        tenantId = tenantId,
//        attributeId = attributeId,
//        xForwardedFor = ip
//      )(BearerToken(bearerToken))
//    invoker.invoke(request, s"Revoking verified attribute $attributeId to $tenantId")
//  }
//}
