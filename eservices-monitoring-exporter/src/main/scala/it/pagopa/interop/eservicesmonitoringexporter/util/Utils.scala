package it.pagopa.interop.eservicesmonitoringexporter.util

import it.pagopa.interop.catalogmanagement.{model => DependencyCatalog}
import it.pagopa.interop.eservicesmonitoringexporter.model._
import it.pagopa.interop.eservicesmonitoringexporter.model.State.{ACTIVE, INACTIVE}
import it.pagopa.interop.eservicesmonitoringexporter.model.Technology.{REST, SOAP}

object Utils {

  implicit class TechnologyWrapper(private val tech: DependencyCatalog.CatalogItemTechnology) extends AnyVal {
    def toApi: Technology = tech match {
      case DependencyCatalog.Rest => REST
      case DependencyCatalog.Soap => SOAP
    }
  }

  implicit class StateWrapper(private val tech: DependencyCatalog.CatalogDescriptorState) extends AnyVal {
    def toApi: State = tech match {
      case DependencyCatalog.Published  => ACTIVE
      case DependencyCatalog.Deprecated => ACTIVE
      case _                            => INACTIVE
    }
  }

  implicit class EServiceDBWrapper(private val e: EServiceDB) extends AnyVal {
    def toPersistent: Seq[EService] =
      e.descriptors.map(descriptor =>
        EService(
          name = e.name,
          eserviceId = e.id,
          versionId = descriptor.id,
          technology = e.technology.toApi,
          state = descriptor.state.toApi,
          basePath = descriptor.serverUrls,
          producerName = e.producerName,
          versionNumber = descriptor.version.toInt,
          audience = descriptor.audience
        )
      )
  }
}
