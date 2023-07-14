package it.pagopa.interop.tenantscertifiedattributesupdater.util

import it.pagopa.interop.tenantscertifiedattributesupdater.util.Utils.minimumInstitutionsThreshold

object Errors {
  final case class InvalidInstitutionsCount(count: Int)
      extends Exception(
        s"Institutions count should be greater than ${minimumInstitutionsThreshold.toString}, current value ${count.toString}"
      )
}
