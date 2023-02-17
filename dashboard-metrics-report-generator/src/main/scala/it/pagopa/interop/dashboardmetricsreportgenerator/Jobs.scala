package it.pagopa.interop.dashboardmetricsreportgenerator

import it.pagopa.interop.commons.files.service.FileManager
import it.pagopa.interop.commons.cqrs.service.ReadModelService
import it.pagopa.interop.catalogmanagement.{model => CatalogManagement}
import it.pagopa.interop.agreementmanagement.model.{agreement => AgreementManagement}
import it.pagopa.interop.purposemanagement.model.{purpose => PurposeManagement}
import it.pagopa.interop.tenantmanagement.model.{tenant => TenantManagement}
import it.pagopa.interop.purposemanagement.model.persistence.JsonFormats._
import it.pagopa.interop.catalogmanagement.model.persistence.JsonFormats._
import it.pagopa.interop.agreementmanagement.model.persistence.JsonFormats._
import it.pagopa.interop.tenantmanagement.model.persistence.JsonFormats._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import org.mongodb.scala.model.Filters
import java.util.UUID
import it.pagopa.interop.commons.utils.TypeConversions._
import java.time.OffsetDateTime
import spray.json._
import it.pagopa.interop.commons.utils.errors.GenericComponentErrors.GenericError

object Jobs {

  def getDescriptorData(readModel: ReadModelService, config: CollectionsConfiguraion): Future[DescriptorsData] =
    getAll(50)(readModel.find[CatalogManagement.CatalogItem](config.eservices, Filters.empty(), _, _)).map {
      eservices =>
        val activeStates: List[CatalogManagement.CatalogDescriptorState] =
          List(CatalogManagement.Published, CatalogManagement.Suspended, CatalogManagement.Deprecated)
        def isActive(d: CatalogManagement.CatalogDescriptor): Boolean    = activeStates.contains(d.state)

        val descriptorAndProducerId: Seq[(CatalogManagement.CatalogDescriptor, UUID)] = for {
          eservice   <- eservices
          descriptor <- eservice.descriptors
        } yield (descriptor, eservice.producerId)

        val activeDescriptors: Int             = descriptorAndProducerId.filter { case (d, _) => isActive(d) }.size
        val producersWithAnActiveEservice: Int = descriptorAndProducerId.distinctBy(_._2).size

        val descriptorsOverTime: List[GraphElement] =
          Graph.getGraphPoints(10)(descriptorAndProducerId.map({ case (d, _) => d.createdAt }))

        DescriptorsData(activeDescriptors, producersWithAnActiveEservice, descriptorsOverTime)
    }

  def getTenantsData(
    readModel: ReadModelService,
    partyManagementProxy: PartyManagementProxy,
    config: CollectionsConfiguraion
  ): Future[TenantsData] =
    getAll(50)(readModel.find[TenantManagement.PersistentTenant](config.tenants, Filters.empty(), _, _))
      .flatMap { tenants =>
        for {
          selfcareIds     <- Future.traverse(tenants.flatMap(_.selfcareId.toList).toList)(_.toFutureUUID)
          onBoardingDates <- Future.parCollectWithLatch(10)(selfcareIds)(partyManagementProxy.getOnboardingDate)
        } yield onBoardingDates
      }
      .map { onBoardingDates =>
        val twoWeeksAgo: OffsetDateTime = OffsetDateTime.now().minusDays(14)
        TenantsData(
          onBoardingDates.size,
          onBoardingDates.count(_.isAfter(twoWeeksAgo)),
          Graph.getGraphPoints(10)(onBoardingDates)
        )
      }

  def getAgreementsData(readModel: ReadModelService, config: CollectionsConfiguraion): Future[AgreementsData] = {
    getAll(50)(readModel.find[AgreementManagement.PersistentAgreement](config.agreements, Filters.empty(), _, _)).map {
      agreements =>
        val activeStates         = List(AgreementManagement.Suspended, AgreementManagement.Active)
        val everBeenActiveStates = AgreementManagement.Archived :: activeStates

        def isActive(a: AgreementManagement.PersistentAgreement): Boolean          = activeStates.contains(a.state)
        def hasEverBeenActive(a: AgreementManagement.PersistentAgreement): Boolean =
          everBeenActiveStates.contains(a.state)

        val actuallyActiveAgreements: Int = agreements.count(isActive)
        val differentConsumers: Int       = agreements.filter(hasEverBeenActive).map(_.producerId).distinct.size

        val activeAgreementsOverTime: List[GraphElement] = Graph.getGraphPoints(10)(
          agreements.filter(hasEverBeenActive).map(a => a.stamps.activation.map(_.when).getOrElse(a.createdAt))
        )

        AgreementsData(actuallyActiveAgreements, differentConsumers, activeAgreementsOverTime)
    }
  }

  def getPurposesData(readModel: ReadModelService, config: CollectionsConfiguraion): Future[PurposesData] =
    getAll(50)(readModel.find[PurposeManagement.PersistentPurpose](config.purposes, Filters.empty(), _, _)).map {
      purposes =>
        val activeStates: List[PurposeManagement.PersistentPurposeVersionState] =
          List(PurposeManagement.Active, PurposeManagement.Suspended, PurposeManagement.Archived)

        def hasEverBeenPublished(p: PurposeManagement.PersistentPurpose): Boolean =
          p.versions.map(_.state).exists(activeStates.contains)

        val publishedPurposes: Seq[PurposeManagement.PersistentPurpose] = purposes.filter(hasEverBeenPublished)
        val differentConsumers: Int = publishedPurposes.map(_.consumerId).distinct.size

        val publishedPurposesOverTime: List[GraphElement] =
          Graph.getGraphPoints(10)(publishedPurposes.map(_.createdAt))

        PurposesData(publishedPurposes.size, differentConsumers, publishedPurposesOverTime)
    }

  def getTokensData(readModel: FileManager, config: TokensBucketConfiguration): Future[TokensData] = {

    def getIssueDate(token: String): Future[OffsetDateTime] =
      Future(token.parseJson)
        .flatMap(js => Future(js.asJsObject))
        .flatMap(_.fields.get("issuedAt").toFuture(GenericError("Missing issuedAt field in token")))
        .flatMap {
          case JsNumber(value) => Future.successful(value.toLong)
          case _               => Future.failed(GenericError("issuedAt should be a number"))
        }
        .flatMap(_.toOffsetDateTime.toFuture)

    readModel
      .getAllFiles(config.bucket)(config.basePath)
      .map(_.values.flatMap(new String(_).split('\n')).toList)
      .flatMap(tokens => Future.traverse(tokens)(getIssueDate))
      .map { issueTimes =>
        val twoWeeksAgo: OffsetDateTime = OffsetDateTime.now().minusDays(14)
        TokensData(issueTimes.size, issueTimes.count(_.isAfter(twoWeeksAgo)), Graph.getGraphPoints(10)(issueTimes))
      }
  }

  private def getAll[T](limit: Int)(get: (Int, Int) => Future[Seq[T]]): Future[Seq[T]] = {
    def go(offset: Int)(acc: Seq[T]): Future[Seq[T]] = {
      get(offset, limit).flatMap(xs =>
        if (xs.size < limit) Future.successful(xs ++ acc)
        else go(offset + xs.size)(xs ++ acc)
      )
    }
    go(0)(Nil)
  }

}
