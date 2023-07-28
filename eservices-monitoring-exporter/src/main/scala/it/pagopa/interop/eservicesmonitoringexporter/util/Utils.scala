package it.pagopa.interop.eservicesmonitoringexporter.util

import it.pagopa.interop.eservicesmonitoringexporter.model._
import it.pagopa.interop.catalogmanagement.{model => DependencyCatalog}
import it.pagopa.interop.eservicesmonitoringexporter.model.State.{ACTIVE, INACTIVE}
import it.pagopa.interop.eservicesmonitoringexporter.model.Technology.{REST, SOAP}

import java.util.UUID

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
    def toPersistent(allowedProducers: Option[List[UUID]]): EService =
      EService(
        name = e.name,
        eserviceId = e.id,
        versionId = e.descriptor.id,
        technology = e.technology.toApi.toString,
        state = allowedProducers.fold(e.descriptor.state.toApi.toString)(
          _.find(_ == e.producerId).map(_ => e.descriptor.state.toApi.toString).getOrElse(INACTIVE.toString)
        ),
        basePath = e.descriptor.serverUrls,
        producerName = e.producerName,
        versionNumber = e.descriptor.version.toInt,
        audience = e.descriptor.audience
      )
  }
}
