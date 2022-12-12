package it.pagopa.interop.metricsreportgenerator.repository.impl

import it.pagopa.interop.catalogmanagement.model.CatalogItem
import it.pagopa.interop.catalogmanagement.model.persistence.JsonFormats._
import it.pagopa.interop.metricsreportgenerator.repository.{CatalogRepository, extractData}
import it.pagopa.interop.metricsreportgenerator.util.ApplicationConfiguration
import org.mongodb.scala.{Document, MongoClient, MongoCollection}

import scala.concurrent.Future

class CatalogRepositoryImpl(client: MongoClient) extends CatalogRepository {

  private val collection: MongoCollection[Document] =
    client
      .getDatabase(ApplicationConfiguration.databaseName)
      .getCollection(ApplicationConfiguration.catalogCollection)

  override def getEServices: Future[Seq[Either[Throwable, CatalogItem]]] =
    collection.find().map(extractData[CatalogItem]).toFuture()
}
