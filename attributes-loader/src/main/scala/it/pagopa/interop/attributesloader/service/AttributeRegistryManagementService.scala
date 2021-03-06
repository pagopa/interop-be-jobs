package it.pagopa.interop.attributesloader.service

import scala.concurrent.Future

trait AttributeRegistryManagementService {

  def loadCertifiedAttributes(bearerToken: String)(implicit contexts: Seq[(String, String)]): Future[Unit]

}
