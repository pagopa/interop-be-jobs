package it.pagopa.interop.attributesloader.service.impl

import it.pagopa.interop.attributeregistrymanagement.client.api.AttributeApi
import it.pagopa.interop.attributeregistrymanagement.client.invoker.{ApiRequest, BearerToken}
import it.pagopa.interop.attributesloader.service.{
  AttributeRegistryManagementInvoker,
  AttributeRegistryManagementService
}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.Future

final case class AttributeRegistryManagementServiceImpl(invoker: AttributeRegistryManagementInvoker, api: AttributeApi)
    extends AttributeRegistryManagementService {

  implicit val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def loadCertifiedAttributes(bearerToken: String): Future[Unit] = {
    val request: ApiRequest[Unit] = api.loadCertifiedAttributes()(BearerToken(bearerToken))
    invoker.invoke(request, s"Loading certificated attributes")
  }
}
