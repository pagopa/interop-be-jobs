package it.pagopa.interop.tenantsattributeschecker.util

import it.pagopa.interop.tenantmanagement.model.tenant.{
  PersistentCertifiedAttribute,
  PersistentDeclaredAttribute,
  PersistentTenantAttribute,
  PersistentTenantRevoker,
  PersistentTenantVerifier,
  PersistentVerifiedAttribute
}
import it.pagopa.interop.agreementprocess.client.{model => AgreementProcess}

object Adapters {

  implicit class DependencyTenantAttributeWrapper(private val t: PersistentTenantAttribute) extends AnyVal {

    def toAgreementApi: AgreementProcess.TenantAttribute = t match {
      case a: PersistentCertifiedAttribute =>
        AgreementProcess.TenantAttribute(certified = Some(a.toAgreementApi))
      case a: PersistentDeclaredAttribute  =>
        AgreementProcess.TenantAttribute(declared = Some(a.toAgreementApi))
      case a: PersistentVerifiedAttribute  =>
        AgreementProcess.TenantAttribute(verified = Some(a.toAgreementApi))
    }

  }

  implicit class DependencyDeclaredTenantAttributeWrapper(private val a: PersistentDeclaredAttribute) extends AnyVal {

    def toAgreementApi: AgreementProcess.DeclaredTenantAttribute =
      AgreementProcess.DeclaredTenantAttribute(
        id = a.id,
        assignmentTimestamp = a.assignmentTimestamp,
        revocationTimestamp = a.revocationTimestamp
      )
  }

  implicit class DependencyCertifiedTenantAttributeWrapper(private val a: PersistentCertifiedAttribute) extends AnyVal {

    def toAgreementApi: AgreementProcess.CertifiedTenantAttribute = AgreementProcess.CertifiedTenantAttribute(
      id = a.id,
      assignmentTimestamp = a.assignmentTimestamp,
      revocationTimestamp = a.revocationTimestamp
    )
  }

  implicit class DependencyVerifiedTenantAttributeWrapper(private val a: PersistentVerifiedAttribute) extends AnyVal {

    def toAgreementApi: AgreementProcess.VerifiedTenantAttribute = AgreementProcess.VerifiedTenantAttribute(
      id = a.id,
      assignmentTimestamp = a.assignmentTimestamp,
      verifiedBy = a.verifiedBy.map(_.toAgreementApi),
      revokedBy = a.revokedBy.map(_.toAgreementApi)
    )
  }

  implicit class DependencyTenantVerifierWrapper(private val t: PersistentTenantVerifier) extends AnyVal {
    def toAgreementApi: AgreementProcess.TenantVerifier = AgreementProcess.TenantVerifier(
      id = t.id,
      verificationDate = t.verificationDate,
      expirationDate = t.expirationDate,
      extensionDate = t.extensionDate
    )
  }

  implicit class DependencyTenantRevokerWrapper(private val t: PersistentTenantRevoker) extends AnyVal {

    def toAgreementApi: AgreementProcess.TenantRevoker = AgreementProcess.TenantRevoker(
      id = t.id,
      verificationDate = t.verificationDate,
      expirationDate = t.expirationDate,
      extensionDate = t.extensionDate,
      revocationDate = t.revocationDate
    )
  }
}
