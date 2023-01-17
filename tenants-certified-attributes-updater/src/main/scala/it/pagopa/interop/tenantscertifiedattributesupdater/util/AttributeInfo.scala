package it.pagopa.interop.tenantscertifiedattributesupdater.util

import it.pagopa.interop.tenantmanagement.model.tenant.{PersistentCertifiedAttribute, PersistentTenantAttribute}
import it.pagopa.interop.tenantprocess.client.model.InternalAttributeSeed

import java.time.OffsetDateTime
import java.util.UUID

final case class AttributeInfo(origin: String, code: String, revocationTimestamp: Option[OffsetDateTime]) {
  def toInternalAttributeSeed: InternalAttributeSeed = InternalAttributeSeed(origin, code)
}

object AttributeInfo {
  def addRevocationTimeStamp(
    attribute: PersistentTenantAttribute,
    attributesIndex: Map[UUID, AttributeInfo]
  ): Option[AttributeInfo] =
    attribute match {
      case PersistentCertifiedAttribute(id, _, revocationTimestamp) =>
        attributesIndex.get(id).map(_.copy(revocationTimestamp = revocationTimestamp))
      case _                                                        => attributesIndex.get(attribute.id)
    }

  def stillExistsInTenant(attributeFromRegistry: AttributeInfo): List[AttributeInfo] => Boolean =
    attributesFromTenant =>
      attributesFromTenant.exists(attributeFromTenant =>
        attributeFromTenant.code == attributeFromRegistry.code &&
          attributeFromTenant.origin == attributeFromRegistry.origin &&
          attributeFromTenant.revocationTimestamp.isEmpty
      )

}
