package it.pagopa.interop.tenantsattributeschecker.service

import it.pagopa.interop.tenantprocess.client.model.{Tenant, UpdateVerifiedTenantAttributeSeed}

import java.util.UUID
import scala.concurrent.Future

trait TenantProcessService {

  def updateVerifiedAttribute(tenantId: UUID, attributeId: UUID, seed: UpdateVerifiedTenantAttributeSeed)(implicit
    contexts: Seq[(String, String)]
  ): Future[Tenant]
}
