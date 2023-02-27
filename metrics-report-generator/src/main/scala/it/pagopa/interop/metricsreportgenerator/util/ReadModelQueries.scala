package it.pagopa.interop.metricsreportgenerator.util

import it.pagopa.interop.commons.cqrs.service.ReadModelService
import it.pagopa.interop.commons.utils.TypeConversions.OptionOps
import it.pagopa.interop.metricsreportgenerator.models.{Agreement, Purpose}
import it.pagopa.interop.metricsreportgenerator.util.Error.TenantNotFound
import it.pagopa.interop.tenantmanagement.model.persistence.JsonFormats._
import it.pagopa.interop.tenantmanagement.model.tenant.PersistentTenant
import org.mongodb.scala.Document
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Aggregates._
import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.Projections._

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

object ReadModelQueries {

  def getTenant()(tenantId: UUID, collections: CollectionsConfiguration)(implicit
    ec: ExecutionContext,
    readModelService: ReadModelService
  ): Future[PersistentTenant] =
    readModelService
      .findOne[PersistentTenant](collections.tenants, Filters.eq("data.id", tenantId.toString))
      .flatMap(_.toFuture(TenantNotFound(tenantId)))

  def getActiveAgreements(offset: Int, limit: Int, collections: CollectionsConfiguration)(implicit
    ec: ExecutionContext,
    readModelService: ReadModelService
  ): Future[Seq[Agreement]] = {
    val firstProjection: Bson = project(
      fields(
        fields(
          include("data"),
          computed("eservice", Document("""{ "$arrayElemAt" : ["$data.eservice.data",0]}""".stripMargin))
        ),
        fields(
          include("data"),
          computed("producer", Document("""{ "$arrayElemAt" : ["$data.producer.data.externalId",0]}""".stripMargin))
        ),
        fields(
          include("data"),
          computed("consumer", Document("""{ "$arrayElemAt" : ["$data.consumer.data.externalId",0]}""".stripMargin))
        ),
        computed("activationDate", "$data.stamps.activation.when"),
        computed("agreementId", "$data.id"),
        computed("eserviceId", "$data.eserviceId"),
        computed("consumerId", "$data.consumerId")
      )
    )

    val secondProjection: Bson = project(
      fields(
        computed("data.eservice", "$eservice.name"),
        computed("data.producer", "$producer.value"),
        computed("data.consumer", "$consumer.value"),
        computed("data.activationDate", "$activationDate"),
        computed("data.agreementId", "$agreementId"),
        computed("data.eserviceId", "$eserviceId"),
        computed("data.consumerId", "$consumerId"),
        exclude("_id")
      )
    )

    val aggregation: Seq[Bson] =
      Seq(
        `match`(Filters.eq("data.state", "Active")),
        lookup("eservices", localField = "data.eserviceId", foreignField = "data.id", as = "data.eservice"),
        lookup("tenants", localField = "data.consumerId", foreignField = "data.id", as = "data.consumer"),
        lookup("tenants", localField = "data.producerId", foreignField = "data.id", as = "data.producer"),
        firstProjection,
        secondProjection
      )

    readModelService.aggregate[Agreement](collections.agreements, aggregation, offset, limit)
  }

  def getPurposes(offset: Int, limit: Int, collections: CollectionsConfiguration)(implicit
    ec: ExecutionContext,
    readModelService: ReadModelService
  ): Future[Seq[Purpose]] = {
    val projection: Bson = project(
      fields(
        computed("data.purposeId", "$data.id"),
        computed("data.consumerId", "$data.consumerId"),
        computed("data.eserviceId", "$data.eserviceId"),
        computed("data.name", "$data.title"),
        exclude("_id")
      )
    )
    readModelService.aggregate[Purpose](collections.purposes, Seq(projection), offset, limit)
  }

}
