package it.pagopa.interop.attributesloader.service.impl

import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.attributeregistryprocess.client.api.AttributeApi
import it.pagopa.interop.attributeregistryprocess.client.invoker.{ApiRequest, BearerToken}
import it.pagopa.interop.attributeregistryprocess.client.model.{Attribute, AttributeSeed}
import it.pagopa.interop.attributesloader.service.{AttributeRegistryProcessInvoker, AttributeRegistryProcessService}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.withHeaders

import scala.concurrent.Future

final case class AttributeRegistryProcessServiceImpl(invoker: AttributeRegistryProcessInvoker, api: AttributeApi)
    extends AttributeRegistryProcessService {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  override def createAttribute(
    attributeSeed: AttributeSeed
  )(implicit contexts: Seq[(String, String)]): Future[Attribute] =
    withHeaders { (bearerToken, correlationId, ip) =>
      val request: ApiRequest[Attribute] = api
        .createAttribute(xCorrelationId = correlationId, xForwardedFor = ip, attributeSeed = attributeSeed)(
          BearerToken(bearerToken)
        )
      invoker.invoke(request, s"Creating attribute")
    }
}
