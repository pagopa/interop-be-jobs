package it.pagopa.interop.dashboardmetricsreportgenerator

import it.pagopa.interop.commons.files.service.FileManager
import it.pagopa.interop.commons.cqrs.service.ReadModelService
import scala.concurrent.Future
import scala.annotation.nowarn
import org.mongodb.scala.model.Filters
import it.pagopa.interop.catalogmanagement.model._
import it.pagopa.interop.tenantmanagement.model.tenant.PersistentTenant
import it.pagopa.interop.catalogmanagement.model.persistence.JsonFormats._
import scala.concurrent.ExecutionContext.Implicits.global
import java.util.UUID
import java.time.OffsetDateTime
import cats.syntax.all._

@nowarn
object Jobs {

  def getDescriptorData(readModel: ReadModelService, config: CollectionsConfiguraion): Future[DescriptorsData] =
    getAll(50)(readModel.find[CatalogItem](config.eservices, Filters.empty(), _, _)).map { eservices =>
      val activeStates: List[CatalogDescriptorState] = List(Published, Suspended, Deprecated)
      def isActive(d: CatalogDescriptor): Boolean    = activeStates.contains(d.state)

      val activeDescriptorCreationDateAndProducerId: Seq[(OffsetDateTime, UUID)] = for {
        eservice   <- eservices
        descriptor <- eservice.descriptors.filter(isActive)
      } yield (descriptor.createdAt, eservice.producerId)

      val activeDescriptors: Int             = activeDescriptorCreationDateAndProducerId.size
      val producersWithAnActiveEservice: Int = activeDescriptorCreationDateAndProducerId.distinctBy(_._2).size

      val descriptorsOverTime: List[GraphElement] =
        Graph.getGraphPoints(10)(activeDescriptorCreationDateAndProducerId.map(_.as(1)))

      DescriptorsData(
        activeDescriptorCreationDateAndProducerId.size,
        producersWithAnActiveEservice,
        descriptorsOverTime
      )
    }

  def getTenantsData(readModel: ReadModelService, config: CollectionsConfiguraion): Future[TenantsData] =
    Future {

      TenantsData(0, 0, Nil)
    }(scala.concurrent.ExecutionContext.global)

  def getAgreementsData(readModel: ReadModelService, config: CollectionsConfiguraion): Future[AgreementsData] =
    Future {

      AgreementsData(0, 0, Nil)
    }(scala.concurrent.ExecutionContext.global)

  def getPurposesData(readModel: ReadModelService, config: CollectionsConfiguraion): Future[PurposesData] =
    Future {

      PurposesData(0, 0, Nil)
    }(scala.concurrent.ExecutionContext.global)

  def getTokensData(readModel: FileManager, config: TokensBucketConfiguration): Future[TokensData] =
    Future {

      TokensData(0, 0, Nil)
    }(scala.concurrent.ExecutionContext.global)

  private def getAll[T](limit: Int)(get: (Int, Int) => Future[Seq[T]]): Future[Seq[T]] = {
    def go(offset: Int)(acc: Seq[T]): Future[Seq[T]] = {
      get(offset, limit).flatMap(xs =>
        if (xs.size < limit) Future.successful(acc)
        else go(offset + xs.size)(xs ++ acc)
      )
    }
    go(0)(Nil)
  }

}
