package it.pagopa.interop.tenantsattributeschecker.util

import cats.implicits.{catsSyntaxOptionId, toFunctorFilterOps, toTraverseOps}
import it.pagopa.interop.tenantmanagement.model.tenant.{
  PersistentTenant,
  PersistentVerificationRenewal,
  PersistentVerifiedAttribute
}
import it.pagopa.interop.tenantsattributeschecker.ApplicationConfiguration.{actorSystem, blockingEc, context}
import it.pagopa.interop.tenantsattributeschecker.service.impl.{AgreementProcessServiceImpl, TenantProcessServiceImpl}
import it.pagopa.interop.tenantsattributeschecker.service.{AgreementProcessService, TenantProcessService}

import scala.concurrent.Future

object jobs {

  private val agreementProcess: AgreementProcessService =
    AgreementProcessServiceImpl(blockingEc)
  private val tenantProcess: TenantProcessService       =
    TenantProcessServiceImpl(blockingEc)

  def applyStrategy(tenants: List[PersistentTenant]): Future[Unit] = {

    val result = for {
      tenant     <- tenants
      attribute  <- tenant.attributes.mapFilter {
        case v: PersistentVerifiedAttribute => v.some
        case _                              => Option.empty[PersistentVerifiedAttribute]
      }
      verifiedBy <- attribute.verifiedBy
    } yield (tenant.id, attribute.id, verifiedBy.id, verifiedBy.renewal)

    result
      .traverse {
        case (tenantId, attributeId, verifierId, PersistentVerificationRenewal.AUTOMATIC_RENEWAL) =>
          tenantProcess.updateVerifiedAttributeExtensionDate(tenantId, attributeId, verifierId)
        case (tenantId, attributeId, _, PersistentVerificationRenewal.REVOKE_ON_EXPIRATION)       =>
          agreementProcess.computeAgreementsByAttribute(tenantId, attributeId)
      }
      .map(_ => ())
  }
}
