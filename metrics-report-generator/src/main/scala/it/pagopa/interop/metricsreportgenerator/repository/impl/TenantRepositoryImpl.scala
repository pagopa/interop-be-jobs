package it.pagopa.interop.metricsreportgenerator.repository.impl

import it.pagopa.interop.metricsreportgenerator.repository.{TenantRepository, extractData}
import it.pagopa.interop.metricsreportgenerator.util.ApplicationConfiguration
import it.pagopa.interop.tenantmanagement.model.tenant.PersistentTenant
import it.pagopa.interop.tenantmanagement.model.persistence.JsonFormats._
import it.pagopa.interop.commons.utils.TypeConversions.EitherOps
import org.mongodb.scala.model.Filters
import org.mongodb.scala.{Document, MongoClient, MongoCollection}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class TenantRepositoryImpl(client: MongoClient) extends TenantRepository {

  private val collection: MongoCollection[Document] =
    client
      .getDatabase(ApplicationConfiguration.databaseName)
      .getCollection(ApplicationConfiguration.tenantCollection)

  def getTenant(tenantId: UUID)(implicit ec: ExecutionContext): Future[PersistentTenant] =
    collection
      .find(Filters.eq("data.id", tenantId.toString))
      .first()
      .toFuture()
      .flatMap(extractData[PersistentTenant](_).toFuture)

}
