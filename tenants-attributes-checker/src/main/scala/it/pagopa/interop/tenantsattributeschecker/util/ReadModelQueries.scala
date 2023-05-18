package it.pagopa.interop.tenantsattributeschecker.util

import it.pagopa.interop.commons.cqrs.service.MongoDbReadModelService
import it.pagopa.interop.tenantsattributeschecker.ApplicationConfiguration._
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Aggregates._
import org.mongodb.scala.model.Filters._

import scala.concurrent.Future

object ReadModelQueries {

  def getAllExpiredAttributesTenants(rm: MongoDbReadModelService): Future[Seq[Tenant]] =
    getAll(100)(getExpiredAttributesTenants(_, _, rm))

  def getExpiredAttributesTenants(
    offset: Int,
    limit: Int,
    readModelService: MongoDbReadModelService
  ): Future[Seq[Tenant]] = {

    val aggregation: List[Bson] = List(
      unwind("$data.attributes"),
      `match`(
        lte(
          "data.attributes.verifiedBy.verificationDate", // TODO rimpiazzare con extensionDate
          dateTimeSupplier.get().toString
        )
      )
    )

    readModelService.aggregate[Tenant](tenantsCollection, aggregation, offset, limit)
  }

  private def getAll[T](limit: Int)(get: (Int, Int) => Future[Seq[T]]): Future[Seq[T]] = {
    def go(offset: Int)(acc: Seq[T]): Future[Seq[T]] = get(offset, limit).flatMap(xs =>
      if (xs.size < limit) Future.successful(xs ++ acc)
      else go(offset + xs.size)(xs ++ acc)
    )
    go(0)(Nil)
  }

}
