package it.pagopa.interop.tenantscertifiedattributesupdater.service

import it.pagopa.interop.tenantprocess.client.model.InternalTenantSeed
import it.pagopa.interop.tenantmanagement.model.tenant.PersistentExternalId
import it.pagopa.interop.tenantscertifiedattributesupdater.util.AttributeInfo

import scala.concurrent.Future

trait TenantProcessService {
  def upsertTenant(bearerToken: String)(seed: InternalTenantSeed)(implicit
    contexts: Seq[(String, String)]
  ): Future[Unit]

  def revokeCertifiedAttribute(bearerToken: String)(externalId: PersistentExternalId, attributeInfo: AttributeInfo)(
    implicit contexts: Seq[(String, String)]
  ): Future[Unit]
}
