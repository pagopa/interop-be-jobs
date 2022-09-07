package it.pagopa.interop.tenantscertifiedattributesupdater.util

import it.pagopa.interop.tenantmanagement.model.tenant.PersistentExternalId
import it.pagopa.interop.tenantprocess.client.model.InternalTenantSeed

final case class TenantActions(
  activations: List[InternalTenantSeed],
  revocations: Map[PersistentExternalId, List[AttributeInfo]]
)
