package it.pagopa.interop.tenantscertifiedattributesupdater.util

import it.pagopa.interop.tenantmanagement.model.tenant.{
  PersistentCertifiedAttribute,
  PersistentExternalId,
  PersistentTenantAttribute
}
import it.pagopa.interop.tenantscertifiedattributesupdater.util.Utils.TenantIdOps

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

  def canBeActivated(fromTenant: Map[PersistentExternalId, List[AttributeInfo]]): TenantSeed => Boolean =
    tenantSeed => !canNotBeActivated(fromTenant, tenantSeed)

  private def canNotBeActivated(
    fromTenant: Map[PersistentExternalId, List[AttributeInfo]],
    tenantSeed: TenantSeed
  ): Boolean =
    fromTenant
      .get(tenantSeed.id.toPersistentExternalId)
      .exists(attributesFromTenant =>
        AttributeInfo.existInTenant(
          attributesFromRegistry = tenantSeed.attributesInfo,
          attributesFromTenant = attributesFromTenant,
          skipRevocationTimestamp = false
        )
      )

  def isRevocable(fromRegistry: List[TenantSeed]): (PersistentExternalId, AttributeInfo) => Boolean =
    (tenantId, attributeFromTenant) => !isNotRevocable(fromRegistry, tenantId, attributeFromTenant)

  private def isNotRevocable(
    fromRegistry: List[TenantSeed],
    tenantId: PersistentExternalId,
    attributeFromTenant: AttributeInfo
  ): Boolean =
    fromRegistry.exists { tenantSeed =>
      tenantSeed.id.toPersistentExternalId == tenantId &&
      AttributeInfo.existInTenant(
        attributesFromTenant = List(attributeFromTenant),
        attributesFromRegistry = tenantSeed.attributesInfo,
        skipRevocationTimestamp = true
      )
    }

  private def existInTenant(
    attributesFromRegistry: List[AttributeInfo],
    attributesFromTenant: List[AttributeInfo],
    skipRevocationTimestamp: Boolean
  ): Boolean = attributesFromRegistry.isEmpty || attributesFromRegistry.exists(attributeFromRegistry =>
    AttributeInfo.existsInTenant(attributesFromTenant, attributeFromRegistry, skipRevocationTimestamp)
  )

  private def existsInTenant(
    attributesFromTenant: List[AttributeInfo],
    attributeFromRegistry: AttributeInfo,
    skipRevocationTimestamp: Boolean
  ): Boolean = attributesFromTenant.exists { attributeFromTenant =>
    attributeFromTenant.code == attributeFromRegistry.code &&
    attributeFromTenant.origin == attributeFromRegistry.origin &&
    (attributeFromTenant.revocationTimestamp.isEmpty || skipRevocationTimestamp)
  }

}
