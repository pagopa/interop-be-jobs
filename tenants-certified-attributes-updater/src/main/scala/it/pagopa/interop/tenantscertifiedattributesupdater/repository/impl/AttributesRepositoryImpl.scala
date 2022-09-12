package it.pagopa.interop.tenantscertifiedattributesupdater.repository.impl

import it.pagopa.interop.attributeregistrymanagement.model.persistence.attribute.PersistentAttribute
import it.pagopa.interop.attributeregistrymanagement.model.persistence.JsonFormats._
import it.pagopa.interop.tenantscertifiedattributesupdater.repository.{AttributesRepository, extractData}
import it.pagopa.interop.tenantscertifiedattributesupdater.system.ApplicationConfiguration
import org.mongodb.scala.MongoClient

import scala.concurrent.{ExecutionContext, Future}

final case class AttributesRepositoryImpl(client: MongoClient) extends AttributesRepository {

  private val collection =
    client
      .getDatabase(ApplicationConfiguration.databaseName)
      .getCollection(ApplicationConfiguration.attributesCollection)

  def getAttributes(implicit ec: ExecutionContext): Future[Seq[PersistentAttribute]] = {
    collection.find().toFuture().map(documents => documents.map(extractData[PersistentAttribute]))
  }

}
