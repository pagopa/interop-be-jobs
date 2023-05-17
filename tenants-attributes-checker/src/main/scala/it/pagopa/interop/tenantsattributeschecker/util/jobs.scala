package it.pagopa.interop.tenantsattributeschecker.util

import it.pagopa.interop.tenantmanagement.model.tenant.PersistentVerificationRenewal
import it.pagopa.interop.tenantsattributeschecker.ApplicationConfiguration
import it.pagopa.interop.tenantsattributeschecker.ApplicationConfiguration.{actorSystem, blockingEc, context}
import it.pagopa.interop.tenantsattributeschecker.service.TenantProcessService
import it.pagopa.interop.tenantsattributeschecker.service.impl.TenantProcessServiceImpl

import java.util.UUID

object jobs {

  private val tenantProcess: TenantProcessService =
    TenantProcessServiceImpl(ApplicationConfiguration.tenantProcessURL, blockingEc)

  def applyStrategy(expiredAttributes: Map[UUID, TenantData]): List[String] = {

    val result = for {
      (id, values) <- expiredAttributes
      attribute    <- values.attributesExpired
      verifiedBy   <- attribute.verifiedBy
    } yield verifiedBy.renewal match {
      case PersistentVerificationRenewal.REVOKE_ON_EXPIRATION =>
        tenantProcess.revokeVerifiedAttribute(id, attribute.id)
        Some(attribute.id.toString)
      case PersistentVerificationRenewal.AUTOMATIC_RENEWAL    => {
        None // TODO AGGIORNARE IL NUOVO VALORE SU DOCUMENTDB
      }
    }

    result.flatten.toList
  }
}
