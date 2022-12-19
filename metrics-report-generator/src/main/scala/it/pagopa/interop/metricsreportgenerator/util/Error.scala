package it.pagopa.interop.metricsreportgenerator.util

import java.util.UUID

object Error {
  final case class TenantNotFound(tenantId: UUID) extends Exception(s"Tenant ${tenantId.toString} not found")
}
