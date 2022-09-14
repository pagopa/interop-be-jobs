package it.pagopa.interop.tenantscertifiedattributesupdater.service

import it.pagopa.interop.tenantprocess.client.model.InternalTenantSeed

import scala.concurrent.Future

trait TenantProcessService {
  def upsertTenant(bearerToken: String)(seed: InternalTenantSeed)(implicit
    contexts: Seq[(String, String)]
  ): Future[Unit]
}
