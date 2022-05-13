package it.pagopa.interop.attributesloader.service.impl

import scala.concurrent.{ExecutionContext, Future}
import it.pagopa.interop.attributeregistrymanagement.client.api.AttributeApi
import it.pagopa.interop.attributeregistrymanagement.client.invoker.{ApiRequest, BearerToken}
import it.pagopa.interop.attributesloader.service.{
  AttributeRegistryManagementInvoker,
  AttributeRegistryManagementService
}
import com.typesafe.scalalogging.Logger
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.CORRELATION_ID_HEADER

final case class AttributeRegistryManagementServiceImpl(invoker: AttributeRegistryManagementInvoker, api: AttributeApi)(
  implicit ec: ExecutionContext
) extends AttributeRegistryManagementService {

  implicit val logger = Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  override def loadCertifiedAttributes(bearerToken: String)(implicit contexts: Seq[(String, String)]): Future[Unit] = {
    // If we will have different jobs in the future we might consider using xCorrelationId
    // to log the specific job name that performed this call
    val request: ApiRequest[Unit] = api
      .loadCertifiedAttributes(xCorrelationId =
        contexts.toMap.getOrElse(CORRELATION_ID_HEADER, s"interop-be-jobs-${System.currentTimeMillis()}")
      )(BearerToken(bearerToken))
    invoker.invoke(request, s"Loading certificated attributes")
  }
}
