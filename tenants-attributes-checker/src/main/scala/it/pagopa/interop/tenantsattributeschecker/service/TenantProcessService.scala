package it.pagopa.interop.tenantsattributeschecker.service

import it.pagopa.interop.tenantprocess.client.model.Tenant

import java.util.UUID
import scala.concurrent.Future

trait TenantProcessService {

  def updateVerifiedAttributeExtensionDate(tenantId: UUID, attributeId: UUID, verifierId: UUID)(implicit
    contexts: Seq[(String, String)]
  ): Future[Tenant]

  def getTenant(tenantId: UUID)(implicit contexts: Seq[(String, String)]): Future[Tenant]
}
