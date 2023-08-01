package it.pagopa.interop.eservicedescriptorsarchiver.util

import it.pagopa.interop.commons.utils.errors.ComponentError

import java.util.UUID

object errors {

  final case class AgreementNotFound(agreementId: String)
      extends ComponentError("0001", s"Agreement not found for agreementId $agreementId")

  final case class EserviceNotFound(eserviceId: UUID)
      extends ComponentError("0002", s"Eservice not found for eserviceId ${eserviceId.toString}")

  final case class DescriptorNotFound(descriptorId: UUID)
      extends ComponentError("0003", s"Descriptor not found for descriptorId ${descriptorId.toString}")
}
