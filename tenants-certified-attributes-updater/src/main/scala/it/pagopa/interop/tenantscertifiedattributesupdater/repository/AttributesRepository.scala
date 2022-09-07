package it.pagopa.interop.tenantscertifiedattributesupdater.repository

import it.pagopa.interop.attributeregistrymanagement.model.persistence.attribute.PersistentAttribute

import scala.concurrent.{ExecutionContext, Future}

trait AttributesRepository {
  def getAttributes(implicit ec: ExecutionContext): Future[Seq[PersistentAttribute]]
}
