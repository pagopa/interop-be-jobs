package it.pagopa.interop.tenantsattributeschecker.util

import it.pagopa.interop.tenantmanagement.model.tenant.PersistentVerificationRenewal

object jobs {

  def applyStrategy(renewal: PersistentVerificationRenewal): Unit = {
    renewal match {
      case PersistentVerificationRenewal.REVOKE_ON_EXPIRATION => ???
      case PersistentVerificationRenewal.AUTOMATIC_RENEWAL    => ???
    }
  }

}
