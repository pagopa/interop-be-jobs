package it.pagopa.interop.eservicesfilegenerator.util

import it.pagopa.interop.commons.cqrs.service.ReadModelService
import org.mongodb.scala.model.Aggregates.{`match`, lookup, unwind}
import org.mongodb.scala.model.{Filters, UnwindOptions, Projections}

import scala.concurrent.{ExecutionContext, Future}
import it.pagopa.interop.eservicesfilegenerator.model.EServiceDB
import it.pagopa.interop.eservicesfilegenerator.model.EServiceDB._

object ReadModelQueries {

  def getEServices(offset: Int, limit: Int)(implicit
    ec: ExecutionContext,
    readModelService: ReadModelService
  ): Future[Seq[EServiceDB]] = {

    for {
      eservices <- readModelService.aggregateRaw[EServiceDB](
        "eservices",
        Seq(
          lookup("tenants", "data.producerId", "data.id", "tenants"),
          unwind("$tenants", UnwindOptions().preserveNullAndEmptyArrays(false)),
          `match`(Filters.empty()),
          Projections.fields(
            Projections.excludeId(),
            Projections.include("data.id"),
            Projections.include("data.name"),
            Projections.include("data.technology"),
            Projections.include("tenants.data.name"),
            Projections.include("data.descriptors.serverUrls"),
            Projections.include("data.descriptors.state"),
            Projections.include("data.descriptors.createdAt"),
            Projections.include("data.descriptors.version")
          )
        ),
        offset = offset,
        limit = limit
      )
    } yield eservices
  }
}
