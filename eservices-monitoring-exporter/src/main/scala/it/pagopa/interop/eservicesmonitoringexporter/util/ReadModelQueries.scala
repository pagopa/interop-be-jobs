package it.pagopa.interop.eservicesmonitoringexporter.util

import it.pagopa.interop.catalogmanagement.model.Draft
import it.pagopa.interop.commons.cqrs.service.ReadModelService
import it.pagopa.interop.eservicesmonitoringexporter.model.EServiceDB
import it.pagopa.interop.eservicesmonitoringexporter.model.EServiceDB._
import org.mongodb.scala.Document
import org.mongodb.scala.bson.BsonArray
import org.mongodb.scala.model.Aggregates.{`match`, lookup, project, unwind}
import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.Projections._

import scala.concurrent.{ExecutionContext, Future}

object ReadModelQueries {

  def getEServices(offset: Int, limit: Int)(implicit
    ec: ExecutionContext,
    readModelService: ReadModelService,
    collectionsConfiguration: CollectionsConfiguration
  ): Future[Seq[EServiceDB]] = for {
    eservices <- readModelService.aggregateRaw[EServiceDB](
      collectionsConfiguration.eservices,
      Seq(
        `match`(Filters.ne("data.descriptors.state", Draft.toString)),
        unwind("$data.descriptors"),
        // This should never occur, but dirty data exists in test env
        `match`(Filters.ne("data.descriptors.serverUrls", BsonArray.fromIterable(Nil))),
        lookup(collectionsConfiguration.tenants, "data.producerId", "data.id", "tenants"),
        unwind("$tenants"),
        project(
          fields(
            excludeId(),
            computed("id", "$data.id"),
            computed("name", "$data.name"),
            computed("technology", "$data.technology"),
            computed("producerId", "$tenants.data.id"),
            computed("producerName", "$tenants.data.name"),
            computed(
              "descriptor",
              Document(
                """{"id":"$data.descriptors.id","state":"$data.descriptors.state","serverUrls":"$data.descriptors.serverUrls","audience":"$data.descriptors.audience","version":"$data.descriptors.version"}"""
              )
            )
          )
        )
      ),
      offset = offset,
      limit = limit
    )
  } yield eservices
}
