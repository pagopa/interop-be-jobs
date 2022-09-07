package it.pagopa.interop.tenantscertifiedattributesupdater.repository

import it.pagopa.interop.tenantmanagement.model.tenant.PersistentTenant

import scala.concurrent.{ExecutionContext, Future}

trait TenantRepository {
  def getTenants(implicit ec: ExecutionContext): Future[List[PersistentTenant]]
}
