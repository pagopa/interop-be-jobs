package it.pagopa.interop.purposesarchiver.service

import it.pagopa.interop.agreementprocess.client.model.Agreement

import java.util.UUID
import scala.concurrent.Future

trait AgreementProcessService {

  def getAgreementById(agreementId: UUID)(implicit contexts: Seq[(String, String)]): Future[Agreement]
}
