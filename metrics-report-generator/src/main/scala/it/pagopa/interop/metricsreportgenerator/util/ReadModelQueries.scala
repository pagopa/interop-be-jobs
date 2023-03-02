package it.pagopa.interop.metricsreportgenerator.util

import it.pagopa.interop.commons.cqrs.service.ReadModelService
import it.pagopa.interop.metricsreportgenerator.util.models.{Agreement, Purpose, Descriptor}
import org.mongodb.scala.Document
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Aggregates._
import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.Projections._
import scala.concurrent.{ExecutionContext, Future}

object ReadModelQueries {

  def getAllActiveAgreements(limit: Int)(
    config: CollectionsConfiguration
  )(implicit ec: ExecutionContext, readModelService: ReadModelService): Future[Seq[Agreement]] =
    getAll(limit)(getActiveAgreements(_, _, config))

  def getActiveAgreements(offset: Int, limit: Int, config: CollectionsConfiguration)(implicit
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

    val aggregation: List[Bson] = List(
      `match`(Filters.eq("data.state", "Active")),
      lookup("eservices", localField = "data.eserviceId", foreignField = "data.id", as = "data.eservice"),
      lookup("tenants", localField = "data.consumerId", foreignField = "data.id", as = "data.consumer"),
      lookup("tenants", localField = "data.producerId", foreignField = "data.id", as = "data.producer"),
      firstProjection,
      secondProjection
    )

    readModelService.aggregate[Agreement](config.agreements, aggregation, offset, limit)
  }

  def getAllPurposes(limit: Int)(
    config: CollectionsConfiguration
  )(implicit ec: ExecutionContext, readModelService: ReadModelService): Future[Seq[Purpose]] =
    getAll(limit)(getPurposes(_, _, config))

  private def getPurposes(offset: Int, limit: Int, collections: CollectionsConfiguration)(implicit
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

  def getAllDescriptors(limit: Int)(
    config: CollectionsConfiguration
  )(implicit ec: ExecutionContext, readModelService: ReadModelService): Future[Seq[Descriptor]] =
    getAll(limit)(getDescriptors(_, _, config))

  private def getDescriptors(offset: Int, limit: Int, collections: CollectionsConfiguration)(implicit
    ec: ExecutionContext,
    readModelService: ReadModelService
  ): Future[Seq[Descriptor]] = {
    val projection1: Bson = project(
      fields(
        exclude("_id"),
        computed("name", "$data.name"),
        computed("createdAt", "$data.createdAt"),
        computed("producerId", "$data.producerId"),
        computed(
          "descriptors",
          Document(
            """{$map:{"input":"$data.descriptors","as":"descriptor","in":{"id":"$$descriptor.id","state":"$$descriptor.state"}}}"""
          )
        )
      )
    )

    val unwindStep: Document = Document("""{$unwind:"$descriptors"}""")

    val projection2: Bson = project(
      fields(
        include("name", "createdAt", "producerId"),
        computed("descriptorId", "$descriptors.id"),
        computed("state", "$descriptors.state")
      )
    )

    readModelService
      .aggregate[Descriptor](collections.eservices, Seq(projection1, unwindStep, projection2), offset, limit)
  }

// db.eservices.aggregate(
//   { $project: {
//     "_id":0,
//     "name":"$data.name",
//     "createdAt":"$data.createdAt",
//     "producerId": "$data.producerId",
//     "descriptors": { $map: {
//       "input": "$data.descriptors",
//       "as": "descriptor",
//       "in" : {
//           "id": "$$descriptor.id",
//           "state" : "$$descriptor.state"
//         }
//     } }
//   }},
//   { $unwind : "$descriptors"},
//   { $project: {
//     "name":1,
//     "createdAt":1,
//     "producerId":1,
//     "descriptorId": "$descriptors.id",
//     "state": "$descriptors.state"}
//   }
//   );

// {
//   name: '1 - Test 1.0.20',
//   createdAt: '2022-10-21T12:00:00Z',
//   producerId: '84871fd4-2fd7-46ab-9d22-f6b452f4b3c5',
//   descriptorId: 'd2e9da04-2be1-477f-be10-bfed925173a8',
//   state: 'Draft'
// }

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
