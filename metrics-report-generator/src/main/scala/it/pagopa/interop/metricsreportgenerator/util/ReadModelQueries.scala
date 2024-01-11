package it.pagopa.interop.metricsreportgenerator.util

import it.pagopa.interop.commons.cqrs.service.ReadModelService
import it.pagopa.interop.metricsreportgenerator.util.models.{Agreement, Descriptor, Purpose}
import org.mongodb.scala.Document
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Aggregates._
import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.Projections._

import scala.concurrent.{ExecutionContext, Future}

object ReadModelQueries {

  def getAllActiveAgreements(config: CollectionsConfiguration, rm: ReadModelService)(implicit
    ec: ExecutionContext
  ): Future[Seq[Agreement]] =
    getAll(config.limit)(getActiveAgreements(_, _, config, rm))

  def getAllDescriptors(config: CollectionsConfiguration, rm: ReadModelService)(implicit
    ec: ExecutionContext
  ): Future[Seq[Descriptor]] =
    getAll(config.limit)(getDescriptors(_, _, config, rm))

  def getAllPurposes(config: CollectionsConfiguration, rm: ReadModelService)(implicit
    ec: ExecutionContext
  ): Future[Seq[Purpose]] =
    getAll(config.limit)(getPurposes(_, _, config, rm))

  private def getActiveAgreements(
    offset: Int,
    limit: Int,
    config: CollectionsConfiguration,
    readModelService: ReadModelService
  )(implicit ec: ExecutionContext): Future[Seq[Agreement]] = {
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
        computed("consumerId", "$data.consumerId"),
        computed("producerId", "$data.producerId"),
        computed("state", "$data.state")
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
        computed("data.producerId", "$producerId"),
        computed("data.consumerId", "$consumerId"),
        computed("data.state", "$state"),
        exclude("_id")
      )
    )

    val aggregation: List[Bson] = List(
      `match`(Filters.in("data.state", "Active", "Suspended", "Archived")),
      lookup("eservices", localField = "data.eserviceId", foreignField = "data.id", as = "data.eservice"),
      lookup("tenants", localField = "data.consumerId", foreignField = "data.id", as = "data.consumer"),
      lookup("tenants", localField = "data.producerId", foreignField = "data.id", as = "data.producer"),
      firstProjection,
      secondProjection
    )

    readModelService
      .aggregate[Agreement](
        collectionName = config.agreements,
        pipeline = aggregation,
        offset = offset,
        limit = limit,
        allowDiskUse = true
      )
  }

  private def getPurposes(
    offset: Int,
    limit: Int,
    collections: CollectionsConfiguration,
    readModelService: ReadModelService
  )(implicit ec: ExecutionContext): Future[Seq[Purpose]] = {
    val projection: Bson = project(
      fields(
        computed("data.purposeId", "$data.id"),
        computed("data.consumerId", "$data.consumerId"),
        computed("data.eserviceId", "$data.eserviceId"),
        computed("data.name", "$data.title"),
        exclude("_id")
      )
    )
    readModelService.aggregate[Purpose](
      collectionName = collections.purposes,
      pipeline = Seq(projection),
      offset = offset,
      limit = limit,
      allowDiskUse = true
    )
  }

  private def getDescriptors(
    offset: Int,
    limit: Int,
    collections: CollectionsConfiguration,
    readModelService: ReadModelService
  )(implicit ec: ExecutionContext): Future[Seq[Descriptor]] = {
    val projection1: Bson = project(
      fields(
        exclude("_id"),
        computed("name", "$data.name"),
        computed("createdAt", "$data.createdAt"),
        computed("producerId", "$data.producerId"),
        computed(
          "descriptors",
          Document(
            """{$map:{"input":"$data.descriptors","as":"descriptor","in":{"id":"$$descriptor.id","state":"$$descriptor.state", "checksum": "$$descriptor.interface.checksum", "voucherLifespan": "$$descriptor.voucherLifespan"}}}"""
          )
        )
      )
    )

    val unwindStep: Document = Document("""{$unwind:"$descriptors"}""")

    val zipProducer =
      lookup("tenants", localField = "producerId", foreignField = "data.id", as = "producerTenant")

    val unwindStep2: Document = Document("""{$unwind:"$producerTenant"}""")

    val projection2: Bson = project(
      fields(
        include("name", "createdAt", "producerId"),
        computed("descriptorId", "$descriptors.id"),
        computed("state", "$descriptors.state"),
        computed("checksum", "$descriptors.checksum"),
        computed("voucherLifespan", "$descriptors.voucherLifespan"),
        computed("producer", "$producerTenant.data.name")
      )
    )

    readModelService.aggregateRaw[Descriptor](
      collectionName = collections.eservices,
      pipeline = Seq(projection1, unwindStep, zipProducer, unwindStep2, projection2),
      offset = offset,
      limit = limit,
      allowDiskUse = true
    )
  }

  private def getAll[T](
    limit: Int
  )(get: (Int, Int) => Future[Seq[T]])(implicit ex: ExecutionContext): Future[Seq[T]] = {
    def go(offset: Int)(acc: Seq[T]): Future[Seq[T]] = get(offset, limit).flatMap(xs =>
      if (xs.size < limit) Future.successful(xs ++ acc)
      else go(offset + xs.size)(xs ++ acc)
    )
    go(0)(Nil)
  }

}
