package it.pagopa.interop.metricsreportgenerator.repository

import it.pagopa.interop.tenantmanagement.model.tenant.PersistentTenant

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

trait TenantRepository {
  def getTenant(tenantId: UUID)(implicit ec: ExecutionContext): Future[PersistentTenant]
}
