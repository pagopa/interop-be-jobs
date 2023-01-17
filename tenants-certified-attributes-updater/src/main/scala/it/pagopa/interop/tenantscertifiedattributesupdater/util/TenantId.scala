package it.pagopa.interop.tenantscertifiedattributesupdater.util

import it.pagopa.interop.tenantmanagement.model.tenant.PersistentExternalId
import it.pagopa.interop.tenantprocess.client.model.ExternalId

final case class TenantId(origin: String, value: String, name: String) {
  def toPersistentExternalId: PersistentExternalId = PersistentExternalId(origin, value)

  def toExternalId: ExternalId = ExternalId(origin, value)
}
