package it.pagopa.interop.attributesloader.service.impl

import scala.concurrent.Future
import com.typesafe.scalalogging.Logger
import it.pagopa.interop.catalogmanagement.client.model.EService
import it.pagopa.interop.catalogmanagement.client.api.EServiceApi
import it.pagopa.interop.catalogmanagement.client.invoker.{ApiRequest, BearerToken}
import it.pagopa.interop.attributesloader.service.{CatalogManagementService, CatalogManagementInvoker}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.CORRELATION_ID_HEADER

final case class CatalogManagementServiceImpl(invoker: CatalogManagementInvoker, api: EServiceApi)
    extends CatalogManagementService {

  implicit val logger = Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  override def getAllEServices(bearerToken: String)(implicit contexts: Seq[(String, String)]): Future[Seq[EService]] = {
    val request: ApiRequest[Seq[EService]] = api.getEServices(xCorrelationId =
      contexts.toMap.getOrElse(CORRELATION_ID_HEADER, s"interop-be-jobs-${System.currentTimeMillis()}")
    )(BearerToken(bearerToken))
    invoker.invoke(request, s"Getting all eservices")
  }

  override def moveAttributesToDescriptors(
    eServiceId: String
  )(bearerToken: String)(implicit contexts: Seq[(String, String)]): Future[Unit] = {
    val request: ApiRequest[Unit] = api.moveAttributesToDescriptors(
      xCorrelationId =
        contexts.toMap.getOrElse(CORRELATION_ID_HEADER, s"interop-be-jobs-${System.currentTimeMillis()}"),
      eServiceId = eServiceId
    )(BearerToken(bearerToken))
    invoker.invoke(request, s"Move Attributes To Descriptors for Eservice $eServiceId")
  }

}
