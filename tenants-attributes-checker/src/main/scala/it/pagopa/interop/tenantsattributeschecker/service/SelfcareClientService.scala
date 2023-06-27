package it.pagopa.interop.tenantsattributeschecker.service

import it.pagopa.interop.selfcare.v2.client.model._

import scala.concurrent.Future

trait SelfcareClientService {

  def getInstitution(selfcareId: String)(implicit contexts: Seq[(String, String)]): Future[Institution]

}
