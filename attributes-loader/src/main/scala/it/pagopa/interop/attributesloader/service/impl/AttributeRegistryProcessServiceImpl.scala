package it.pagopa.interop.attributesloader.service.impl

import com.typesafe.scalalogging.Logger
import it.pagopa.interop.attributeregistryprocess.client.api.AttributeApi
import it.pagopa.interop.attributeregistryprocess.client.invoker.{ApiRequest, BearerToken}
import it.pagopa.interop.attributesloader.service.{AttributeRegistryProcessInvoker, AttributeRegistryProcessService}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.CORRELATION_ID_HEADER

import scala.concurrent.Future

final case class AttributeRegistryProcessServiceImpl(invoker: AttributeRegistryProcessInvoker, api: AttributeApi)
    extends AttributeRegistryProcessService {

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
