package it.pagopa.interop.tenantscertifiedattributesupdater.repository.impl

import it.pagopa.interop.tenantmanagement.model.persistence.JsonFormats._
import it.pagopa.interop.tenantmanagement.model.tenant.PersistentTenant
import it.pagopa.interop.tenantscertifiedattributesupdater.repository.{TenantRepository, extractData}
import it.pagopa.interop.tenantscertifiedattributesupdater.system.ApplicationConfiguration
import org.mongodb.scala.{Document, MongoClient, MongoCollection}

import scala.concurrent.Future

final case class TenantRepositoryImpl(client: MongoClient) extends TenantRepository {

  private val collection: MongoCollection[Document] =
    client
      .getDatabase(ApplicationConfiguration.databaseName)
      .getCollection(ApplicationConfiguration.tenantsCollection)

  def getTenants: Future[Seq[Either[Throwable, PersistentTenant]]] =
    collection.find().map(extractData[PersistentTenant]).toFuture()

}
