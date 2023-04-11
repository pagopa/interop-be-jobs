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
      .exists(attributesFromTenant => // check if exist in tenant
        tenantSeed.attributesInfo.isEmpty || tenantSeed.attributesInfo.exists(attributeFromRegistry =>
          attributesFromTenant.exists { attributeFromTenant =>
            attributeFromTenant.code == attributeFromRegistry.code &&
            attributeFromTenant.origin == attributeFromRegistry.origin &&
            attributeFromTenant.revocationTimestamp.isEmpty
          }
        )
      )

  def isRevocable(fromRegistry: List[TenantSeed]): (PersistentExternalId, AttributeInfo) => Boolean =
    (tenantId, attributeFromTenant) => !isNotRevocable(fromRegistry, tenantId, attributeFromTenant)

  private def isNotRevocable(
    fromRegistry: List[TenantSeed],
    tenantId: PersistentExternalId,
    attributeFromTenant: AttributeInfo
  ): Boolean =
    fromRegistry.exists { tenantSeed => // check if exists in registry
      tenantSeed.id.toPersistentExternalId == tenantId && (tenantSeed.attributesInfo.isEmpty || tenantSeed.attributesInfo
        .exists(attributeFromRegistry =>
          attributeFromTenant.code == attributeFromRegistry.code &&
            attributeFromTenant.origin == attributeFromRegistry.origin
        ))
    }

}
