package it.pagopa.interop.tenantsattributeschecker.util

import it.pagopa.interop.tenantmanagement.model.tenant.PersistentVerificationRenewal
import it.pagopa.interop.tenantsattributeschecker.ApplicationConfiguration.{actorSystem, blockingEc, context}
import it.pagopa.interop.tenantsattributeschecker.service.impl.{
  AgreementProcessServiceImpl,
  TenantManagementServiceImpl
}
import it.pagopa.interop.tenantsattributeschecker.service.{AgreementProcessService, TenantManagementService}

import java.time.{Duration, OffsetDateTime}

object jobs {

  private val tenantManagement: TenantManagementService =
    TenantManagementServiceImpl(blockingEc)
  private val agreementProcess: AgreementProcessService =
    AgreementProcessServiceImpl(blockingEc)

  def applyStrategy(tenants: List[TenantData]): Unit = {

    val result = for {
      tenant     <- tenants
      attribute  <- tenant.attributesExpired
      verifiedBy <- attribute.verifiedBy
    } yield verifiedBy.renewal match {
      case PersistentVerificationRenewal.REVOKE_ON_EXPIRATION =>
        agreementProcess.computeAgreementsByAttribute(tenant.id, attribute.id)
      case PersistentVerificationRenewal.AUTOMATIC_RENEWAL    => {
        // TODO AGGIORNARE IL NUOVO VALORE SU DOCUMENTDB

        val newExtensionDate: Option[OffsetDateTime] = for {
          extensionDate  <- verifiedBy.extensionDate
          expirationDate <- verifiedBy.expirationDate
        } yield extensionDate.plus(Duration.between(verifiedBy.verificationDate, expirationDate))

        tenantManagement.updateTenantAttribute(
          tenant.id,
          attribute.id,
          TenantAttribute(verified = verifiedBy.copy(extensionDate = newExtensionDate))
        )
      }
    }

  }
}
