package it.pagopa.interop.eservicesfilegenerator.util

import it.pagopa.interop.catalogmanagement.{model => DependencyCatalog}
import it.pagopa.interop.eservicesfilegenerator.model.{EService, EServiceDB, State, Technology}
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
    def toApi: EService = EService(
      name = e.name,
      eserviceId = e.id,
      versionId = e.descriptors.maxBy(_.createdAt).id,
      technology = e.technology.toApi,
      state = e.descriptors.maxBy(_.createdAt).state.toApi,
      basePath = e.descriptors.maxBy(_.createdAt).serverUrls,
      producerName = e.producerName
    )
  }
}
