package it.pagopa.interop.tenantsattributeschecker.util

import it.pagopa.interop.commons.cqrs.service.MongoDbReadModelService
import it.pagopa.interop.tenantmanagement.model.persistence.JsonFormats.ptFormat
import it.pagopa.interop.tenantmanagement.model.tenant.PersistentTenant
import it.pagopa.interop.tenantsattributeschecker.ApplicationConfiguration._
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Aggregates._
import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.Filters._

import scala.concurrent.Future

object ReadModelQueries {

  def getAllExpiredAttributesTenants(
    rm: MongoDbReadModelService,
    expirationInMonth: Boolean = false
  ): Future[Seq[PersistentTenant]] =
    getAll(100)(getExpiredAttributesTenants(_, _, rm, expirationInMonth))

  private def getExpiredAttributesTenants(
    offset: Int,
    limit: Int,
    readModelService: MongoDbReadModelService,
    expirationInMonth: Boolean
  ): Future[Seq[PersistentTenant]] = {

    val dateFilter: Bson =
      if (expirationInMonth)
        Filters.eq("data.attributes.verifiedBy.extensionDate", dateTimeSupplier.get().plusDays(30).toString)
      else lte("data.attributes.verifiedBy.extensionDate", dateTimeSupplier.get().toString)

    val aggregation: List[Bson] = List(unwind("$data.attributes"), `match`(dateFilter))

    readModelService.aggregate[PersistentTenant](tenantsCollection, aggregation, offset, limit)
  }

  private def getAll[T](limit: Int)(get: (Int, Int) => Future[Seq[T]]): Future[Seq[T]] = {
    def go(offset: Int)(acc: Seq[T]): Future[Seq[T]] = get(offset, limit).flatMap(xs =>
      if (xs.size < limit) Future.successful(xs ++ acc)
      else go(offset + xs.size)(xs ++ acc)
    )
    go(0)(Nil)
  }
}