package it.pagopa.interop.tenantscertifiedattributesupdater.util

import it.pagopa.interop.tenantmanagement.model.tenant.{PersistentCertifiedAttribute, PersistentTenantAttribute}

import java.time.OffsetDateTime
import java.util.UUID

final case class AttributeInfo(origin: String, code: String, revocationTimestamp: Option[OffsetDateTime])

object AttributeInfo {
  def addRevocationTimeStamp(
    attributesIndex: Map[UUID, AttributeInfo]
  ): PersistentTenantAttribute => Option[AttributeInfo] = {
    case PersistentCertifiedAttribute(id, _, revocationTimestamp) =>
      attributesIndex.get(id).map(_.copy(revocationTimestamp = revocationTimestamp))
    case attribute                                                => attributesIndex.get(attribute.id)
  }

}
