package it.pagopa.interop.tenantsattributeschecker.service

import it.pagopa.interop.selfcare.partyprocess.client.model.Institution

import scala.concurrent.{ExecutionContext, Future}

trait PartyProcessService {

  def getInstitution(
    selfcareId: String
  )(implicit contexts: Seq[(String, String)], ec: ExecutionContext): Future[Institution]
}
