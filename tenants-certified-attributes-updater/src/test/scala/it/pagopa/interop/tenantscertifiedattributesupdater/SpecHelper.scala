//package it.pagopa.interop.tenantscertifiedattributesupdater
//
//import it.pagopa.interop.partyregistryproxy.client.model.Institution
//import it.pagopa.interop.tenantmanagement.model.tenant.{
//  PersistentExternalId,
//  PersistentTenant,
//  PersistentTenantAttribute
//}
//
//import java.time.{OffsetDateTime, ZoneOffset}
//import java.util.UUID
//
//object SpecHelper {
//
//  final val timestamp: OffsetDateTime = OffsetDateTime.of(2022, 12, 31, 11, 22, 33, 0, ZoneOffset.UTC)
//
//  def persistentTenant(
//    origin: String,
//    value: String,
//    attributes: List[PersistentTenantAttribute] = Nil
//  ): PersistentTenant = PersistentTenant(
//    id = UUID.randomUUID(),
//    selfcareId = None,
//    externalId = PersistentExternalId(origin, value),
//    features = Nil,
//    attributes = attributes,
//    createdAt = timestamp,
//    updatedAt = None
//  )
//
//  def institution(origin: String, originId: String, category: String): Institution = Institution(
//    id = UUID.randomUUID().toString,
//    originId = originId,
//    o = None,
//    ou = None,
//    aoo = None,
//    taxCode = "taxCode",
//    category = category,
//    description = "",
//    digitalAddress = "",
//    address = "",
//    zipCode = "",
//    origin = origin
//  )
//
//}
