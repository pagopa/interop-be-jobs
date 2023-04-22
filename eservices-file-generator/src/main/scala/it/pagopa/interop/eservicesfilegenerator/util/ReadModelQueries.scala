package it.pagopa.interop.eservicesfilegenerator.util

import it.pagopa.interop.commons.cqrs.service.ReadModelService
import it.pagopa.interop.eservicesfilegenerator.model.EServiceDB
import it.pagopa.interop.eservicesfilegenerator.model.EServiceDB._
import org.mongodb.scala.model.Aggregates.{project, lookup, unwind}
import org.mongodb.scala.Document
import org.mongodb.scala.model.Projections._

import scala.concurrent.{ExecutionContext, Future}

object ReadModelQueries {

  def getEServices(offset: Int, limit: Int)(implicit
    ec: ExecutionContext,
    readModelService: ReadModelService
  ): Future[Seq[EServiceDB]] = for {
    eservices <- readModelService.aggregateRaw[EServiceDB](
      "eservices",
      Seq(
        lookup("tenants", "data.producerId", "data.id", "tenants"),
        unwind("$tenants"),
        project(
          fields(
            excludeId(),
            computed("id", "$data.id"),
            computed("name", "$data.name"),
            computed("technology", "$data.technology"),
            computed("producerName", "$tenants.data.name"),
            computed(
              "descriptors",
              Document(
                """{$map:{"input":"$data.descriptors","as":"descriptor","in":{"id":"$$descriptor.id","state":"$$descriptor.state","serverUrls":"$$descriptor.serverUrls","createdAt":"$$descriptor.createdAt","version":"$$descriptor.version"}}}"""
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
