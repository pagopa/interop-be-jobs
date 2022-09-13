package it.pagopa.interop.tenantscertifiedattributesupdater.repository

import it.pagopa.interop.tenantmanagement.model.tenant.PersistentTenant

import scala.concurrent.Future

trait TenantRepository {
  def getTenants: Future[Seq[PersistentTenant]]
}
