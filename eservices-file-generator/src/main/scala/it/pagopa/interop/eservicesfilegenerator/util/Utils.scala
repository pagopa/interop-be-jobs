package it.pagopa.interop.eservicesfilegenerator.util

import it.pagopa.interop.catalogmanagement.{model => DependencyCatalog}
import it.pagopa.interop.eservicesfilegenerator.model.{EService, EServiceDB, DescriptorDB, State, Technology}
import it.pagopa.interop.eservicesfilegenerator.model.State.{ACTIVE, INACTIVE}
import it.pagopa.interop.eservicesfilegenerator.model.Technology.{REST, SOAP}

object Utils {

  implicit class TechnologyWrapper(private val tech: DependencyCatalog.CatalogItemTechnology) extends AnyVal {
    def toApi: Technology = tech match {
      case DependencyCatalog.Rest => REST
      case DependencyCatalog.Soap => SOAP
    }
  }

  implicit class StateWrapper(private val tech: DependencyCatalog.CatalogDescriptorState) extends AnyVal {
    def toApi: State = tech match {
      case DependencyCatalog.Published => ACTIVE
      case _                           => INACTIVE
    }
  }

  implicit class EServiceDBWrapper(private val e: EServiceDB) extends AnyVal {
    def toApi: EService = {
      val descriptor: DescriptorDB = e.descriptors.maxBy(_.createdAt)

      EService(
        name = e.name,
        eserviceId = e.id,
        versionId = descriptor.id,
        technology = e.technology.toApi,
        state = descriptor.state.toApi,
        basePath = descriptor.serverUrls,
        producerName = e.producerName,
        version = descriptor.version
      )
    }
  }
}
