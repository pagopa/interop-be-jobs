package it.pagopa.interop.tenantsattributeschecker.util

import it.pagopa.interop.commons.utils.errors.ComponentError

import java.util.UUID

object errors {

  final case class SelfcareIdNotFound(tenantId: UUID)
      extends ComponentError("0001", s"Selfcare id not found for tenant ${tenantId.toString}")
  final case class SelfcareEntityNotFilled(className: String, field: String)
      extends ComponentError("0002", s"Selfcare entity $className with field $field not filled")
}
