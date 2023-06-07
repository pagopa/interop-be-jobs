package it.pagopa.interop.padigitalereportgenerator.util

import java.util.UUID

object Error {
  final case class TenantNotFound(tenantId: UUID) extends Exception(s"Tenant ${tenantId.toString} not found")
  final case class MissingActivationTimestamp(descriptorId: UUID)
      extends Exception(s"Descriptor ${descriptorId.toString} without publishedAt field")
}
