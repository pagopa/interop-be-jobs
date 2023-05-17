package it.pagopa.interop.tenantsattributeschecker.service

import it.pagopa.interop.tenantprocess.client.model.Tenant

import java.util.UUID
import scala.concurrent.Future

trait TenantProcessService {

  def revokeVerifiedAttribute(tenantId: UUID, attributeId: UUID)(implicit
    contexts: Seq[(String, String)]
  ): Future[Tenant]
}
