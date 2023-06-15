package it.pagopa.interop.tenantscertifiedattributesupdater

import it.pagopa.interop.partyregistryproxy.client.model.Institution
import it.pagopa.interop.tenantmanagement.model.tenant.{
  PersistentExternalId,
  PersistentTenant,
  PersistentTenantAttribute
//  PersistentTenantKind
}

import java.time.{OffsetDateTime, ZoneOffset}
import java.util.UUID

object SpecHelper {

  final val timestamp: OffsetDateTime = OffsetDateTime.of(2022, 12, 31, 11, 22, 33, 0, ZoneOffset.UTC)

  final val defaultName: String = "test_name"

  def persistentTenant(
    origin: String,
    value: String,
    attributes: List[PersistentTenantAttribute] = Nil
  ): PersistentTenant = PersistentTenant(
    id = UUID.randomUUID(),
//    kind = Some(PersistentTenantKind.PA),
    selfcareId = None,
    externalId = PersistentExternalId(origin, value),
    features = Nil,
    attributes = attributes,
    createdAt = timestamp,
    updatedAt = None,
    mails = Nil,
    name = defaultName
  )

  def institution(origin: String, originId: String, category: String, kind: String): Institution = Institution(
    id = UUID.randomUUID().toString,
    originId = originId,
    o = None,
    ou = None,
    aoo = None,
    taxCode = "taxCode",
    category = category,
    description = defaultName,
    digitalAddress = "",
    address = "",
    zipCode = "",
    origin = origin,
    kind = kind
  )

}
