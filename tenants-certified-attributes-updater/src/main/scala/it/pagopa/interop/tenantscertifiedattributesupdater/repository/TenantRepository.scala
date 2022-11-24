package it.pagopa.interop.tenantscertifiedattributesupdater.repository

import it.pagopa.interop.tenantmanagement.model.tenant.PersistentTenant
import it.pagopa.interop.tenantscertifiedattributesupdater.Main.CompactTenant

import scala.concurrent.Future

trait TenantRepository {
  def getTenants: Future[Seq[Either[Throwable, PersistentTenant]]]
  def getTenantsWithoutName: Future[Seq[Either[Throwable, CompactTenant]]]
}
