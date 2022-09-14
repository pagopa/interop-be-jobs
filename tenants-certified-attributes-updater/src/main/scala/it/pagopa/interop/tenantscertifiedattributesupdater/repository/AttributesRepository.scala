package it.pagopa.interop.tenantscertifiedattributesupdater.repository

import it.pagopa.interop.attributeregistrymanagement.model.persistence.attribute.PersistentAttribute

import scala.concurrent.Future

trait AttributesRepository {
  def getAttributes: Future[Seq[Either[Throwable, PersistentAttribute]]]
}
