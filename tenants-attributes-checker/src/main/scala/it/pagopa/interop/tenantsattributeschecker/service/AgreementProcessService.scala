package it.pagopa.interop.tenantsattributeschecker.service

import it.pagopa.interop.agreementprocess.client.model.CompactTenant

import java.util.UUID
import scala.concurrent.Future

trait AgreementProcessService {

  def computeAgreementsByAttribute(attributeId: UUID, consumer: CompactTenant)(implicit
    contexts: Seq[(String, String)]
  ): Future[Unit]
}
