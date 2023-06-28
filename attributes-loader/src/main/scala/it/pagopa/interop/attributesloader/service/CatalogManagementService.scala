package it.pagopa.interop.attributesloader.service

import scala.concurrent.Future
import it.pagopa.interop.catalogmanagement.client.model.EService

trait CatalogManagementService {

  def getAllEServices(bearerToken: String)(implicit contexts: Seq[(String, String)]): Future[Seq[EService]]

  def moveAttributesToDescriptors(eServiceId: String)(bearerToken: String)(implicit
    contexts: Seq[(String, String)]
  ): Future[Unit]
}
