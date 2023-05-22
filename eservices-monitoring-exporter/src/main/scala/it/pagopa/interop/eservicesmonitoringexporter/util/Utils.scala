package it.pagopa.interop.eservicesmonitoringexporter.util

import it.pagopa.interop.eservicesmonitoringexporter.model._

object Utils {

  implicit class EServiceDBWrapper(private val e: EServiceDB) extends AnyVal {
    def toPersistent: Seq[EService] =
      e.descriptors.map(descriptor =>
        EService(
          name = e.name,
          eserviceId = e.id,
          versionId = descriptor.id,
          technology = e.technology.toString.toUpperCase,
          state = descriptor.state.toString.toUpperCase,
          basePath = descriptor.serverUrls,
          producerName = e.producerName,
          versionNumber = descriptor.version.toInt,
          audience = descriptor.audience
        )
      )
  }
}
