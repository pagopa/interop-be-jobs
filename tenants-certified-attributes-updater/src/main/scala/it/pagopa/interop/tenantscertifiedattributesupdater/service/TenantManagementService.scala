package it.pagopa.interop.tenantscertifiedattributesupdater.service

import it.pagopa.interop.tenantmanagement.client.model.TenantDelta

import java.util.UUID
import scala.concurrent.Future

trait TenantManagementService {
  def updateTenant(bearerToken: String)(tenantId: UUID, delta: TenantDelta)(implicit
    contexts: Seq[(String, String)]
  ): Future[Unit]
}
