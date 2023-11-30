package it.pagopa.interop.tenantattributechecker.utils

import cats.implicits._
import it.pagopa.interop.attributeregistryprocess.client.model.Attribute
import it.pagopa.interop.attributeregistryprocess.client.model.AttributeKind.VERIFIED
import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier
import it.pagopa.interop.tenantmanagement.model.tenant._
import it.pagopa.interop.tenantprocess.client.model._
import it.pagopa.interop.selfcare.v2.client.model.Institution
import it.pagopa.interop.tenantsattributeschecker.service.impl.MailTemplate

import java.time.{OffsetDateTime, ZoneOffset}
import java.util.UUID

trait SpecData {
  final val timestamp = OffsetDateTime.of(2022, 12, 31, 11, 22, 33, 44, ZoneOffset.UTC)

  val (consumerExpiredTemplate, producerExpiredTemplate): (MailTemplate, MailTemplate)   = MailTemplate.expired
  val (consumerExpiringTemplate, producerExpiringTemplate): (MailTemplate, MailTemplate) = MailTemplate.expiring

  val tenantRevoker: TenantRevoker = TenantRevoker(
    id = UUID.randomUUID(),
    verificationDate = timestamp,
    expirationDate = None,
    extensionDate = None,
    revocationDate = timestamp
  )

  val mockAttribute: Attribute = Attribute(
    id = UUID.randomUUID,
    code = Some("205942"),
    kind = VERIFIED,
    description = "205942",
    origin = Some("ALTRO"),
    name = "test",
    creationTime = OffsetDateTimeSupplier.get()
  )

  val mockTenant: Tenant = Tenant(
    id = UUID.randomUUID(),
    selfcareId = UUID.randomUUID().toString.some,
    externalId = ExternalId("IPA", "org"),
    features = Nil,
    attributes = Seq(
      TenantAttribute(verified =
        VerifiedTenantAttribute(
          id = UUID.randomUUID(),
          assignmentTimestamp = timestamp,
          verifiedBy = Seq(
            TenantVerifier(
              id = UUID.randomUUID(),
              verificationDate = timestamp,
              expirationDate = timestamp.some,
              extensionDate = None
            )
          ),
          revokedBy = Seq(tenantRevoker)
        ).some
      )
    ),
    createdAt = timestamp,
    updatedAt = None,
    mails = Nil,
    name = "test_name",
    kind = None
  )

  final lazy val institution = Institution(
    id = Some(UUID.randomUUID()),
    externalId = Some("27f8dce0-0a5b-476b-9fdd-a7a658eb9211"),
    originId = Some("originId1"),
    description = Some("Institution One"),
    digitalAddress = Some("digitalAddress1"),
    address = Some("address1"),
    zipCode = Some("zipCode1"),
    taxCode = Some("taxCode1"),
    origin = Some("origin"),
    attributes = Some(Seq.empty)
  )

  def createPersistentTenant(tenantId: UUID, attributeId: UUID, verifierId: UUID): PersistentTenant = {
    PersistentTenant(
      id = tenantId,
      selfcareId = UUID.randomUUID().toString.some,
      externalId = PersistentExternalId("IPA", "org"),
      features = Nil,
      attributes = List(
        PersistentVerifiedAttribute(
          id = attributeId,
          assignmentTimestamp = timestamp,
          verifiedBy = List(persistentTenantVerifier(verifierId)),
          revokedBy = List(persistentTenantRevoker)
        )
      ),
      createdAt = timestamp,
      updatedAt = None,
      mails = Nil,
      name = "test_name",
      kind = None,
  onboardedAt = None,
  subUnitType = None
    )
  }

  def persistentTenantVerifier(verifierId: UUID): PersistentTenantVerifier = PersistentTenantVerifier(
    id = verifierId,
    verificationDate = timestamp,
    expirationDate = timestamp.plusMonths(1).some,
    extensionDate = timestamp.some
  )

  val persistentTenantRevoker: PersistentTenantRevoker = PersistentTenantRevoker(
    id = UUID.randomUUID(),
    verificationDate = timestamp,
    expirationDate = None,
    extensionDate = None,
    revocationDate = timestamp
  )

}
