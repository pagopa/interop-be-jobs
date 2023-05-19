package it.pagopa.interop.tenantsattributeschecker.util

import it.pagopa.interop.tenantmanagement.model.tenant.{
  PersistentTenant,
  PersistentVerificationRenewal,
  PersistentVerifiedAttribute
}
import it.pagopa.interop.tenantsattributeschecker.ApplicationConfiguration.{actorSystem, blockingEc, context}
import it.pagopa.interop.tenantsattributeschecker.service.impl.{AgreementProcessServiceImpl, TenantProcessServiceImpl}
import it.pagopa.interop.tenantsattributeschecker.service.{AgreementProcessService, TenantProcessService}

import java.time.{Duration, OffsetDateTime}

object jobs {

//  private val tenantManagement: TenantManagementService =
//    TenantManagementServiceImpl(blockingEc)
  private val agreementProcess: AgreementProcessService =
    AgreementProcessServiceImpl(blockingEc)
  private val tenantProcess: TenantProcessService       =
    TenantProcessServiceImpl(blockingEc)

  def applyStrategy(tenants: Seq[PersistentTenant]): Unit = {

    for {
      tenant            <- tenants
      attribute         <- tenant.attributes
      verifiedAttribute <- attribute match {
        case v: PersistentVerifiedAttribute => Some(v)
        case _                              => None
      }
      verifier          <- verifiedAttribute.verifiedBy
    } yield verifier.renewal match {
      case PersistentVerificationRenewal.REVOKE_ON_EXPIRATION =>
        agreementProcess.computeAgreementsByAttribute(tenant.id, attribute.id)
      case PersistentVerificationRenewal.AUTOMATIC_RENEWAL    =>
        // TODO AGGIORNARE IL NUOVO VALORE SU DOCUMENTDB

        val newExtensionDate: Option[OffsetDateTime] = for {
          extensionDate  <- verifier.extensionDate
          expirationDate <- verifier.expirationDate
        } yield extensionDate.plus(Duration.between(verifier.verificationDate, expirationDate))

        tenantProcess.updateVerifiedAttribute(
          tenant.id,
          attribute.id,
          UpdateVerifiedTenantAttributeSeed(extensionDate = newExtensionDate)
        )
    }

  }
}
