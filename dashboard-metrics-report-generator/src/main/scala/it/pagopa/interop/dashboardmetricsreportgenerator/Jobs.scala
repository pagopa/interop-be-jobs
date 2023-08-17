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
import java.time.ZoneId
import scala.util.Try
import cats.syntax.all._
import scala.util.Failure
import scala.util.Success
import java.time._
import java.util.stream.Collectors
import scala.jdk.CollectionConverters._
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object Jobs extends DefaultJsonProtocol {

  private val maxParallelism: Int = 1.max(Runtime.getRuntime().availableProcessors() - 1)

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
    config: CollectionsConfiguraion,
    overrides: Overrides
  ): Future[TenantsData] =
    getAll(50)(readModel.find[TenantManagement.PersistentTenant](config.tenants, Filters.empty(), _, _))
      .flatMap { tenants =>
        for {
          selfcareIds     <- Future.traverse(tenants.flatMap(_.selfcareId.toList).toList)(_.toFutureUUID)
          onBoardingDates <- Future.parCollectWithLatch(maxParallelism)(selfcareIds)(
            partyManagementProxy.getOnboardingDate
          )
        } yield onBoardingDates
      }
      .map { onBoardingDates =>
        TenantsData(
          overrides.totalTenants.map(_.max(onBoardingDates.size)).getOrElse(onBoardingDates.size),
          onBoardingDates.size,
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

  def getTokensData(
    fileManager: FileManager,
    config: TokensBucketConfiguration,
    storageConfiguration: StorageBucketConfiguration
  ): Future[TokensData] = {

    def getTokensReport(): Future[Option[TokensReport]] = fileManager
      .getFile(storageConfiguration.bucket)(storageConfiguration.tokensPartialReportFilename)
      .transform {
        case Failure(_) => Success(Option.empty[TokensReport])
        case Success(s) => TokensReport.from(new String(s)).map(_.some)
      }

    def datesUntilToday(startDate: LocalDate): List[LocalDate] = startDate
      .datesUntil(LocalDate.now())
      .collect(Collectors.toList[LocalDate]())
      .asScala
      .toList
      .appended(LocalDate.now())

    def getIssueDate(token: String): Try[OffsetDateTime] = Try {
      token.parseJson.asJsObject.fields.get("issuedAt").fold(throw GenericError("Missing issuedAt field in token")) {
        case JsNumber(value) => value.toLong.toEuropeRomeOffsetDateTime.get
        case _               => throw GenericError("issuedAt should be a number")
      }
    }

    def allTokensPaths(): Future[List[fileManager.StorageFilePath]] =
      fileManager.listFiles(config.bucket)(config.basePath)

    def getTokenLines(path: String): Future[List[String]] =
      fileManager.getFile(config.bucket)(path).map(bs => new String(bs).split('\n').toList)

    val threeDaysAgo: OffsetDateTime =
      OffsetDateTime.now(ZoneId.of("Europe/Rome")).truncatedTo(ChronoUnit.DAYS).minusDays(3)

    def updateReport(afterThan: Instant, beforeThan: Instant)(
      report: TokensReport
    )(tokenFilesPath: List[String]): Future[TokensReport] = {

      Future
        .accumulateLeft(10)(tokenFilesPath)(getTokenLines)(Try(report)) { case (tryReport, records) =>
          for {
            r     <- tryReport
            times <- records.traverse(getIssueDate)
            timesToAdd = times.filter(i => i.toInstant.isAfter(afterThan) && i.toInstant.isBefore(beforeThan))
          } yield timesToAdd.foldLeft(r)(_.addDate(_))
        }
        .flatMap(_.toFuture)
    }

    def listTokensForDay(date: LocalDate): Future[List[String]] = {
      val path = config.basePath
        .stripPrefix("/")
        .stripSuffix("/")
        .concat("/")
        .concat(date.format(DateTimeFormatter.BASIC_ISO_DATE))

      println(s"Getting tokens at ${config.bucket} ${path}")

      fileManager.listFiles(config.bucket)(path)
    }

    val beforeThan: Instant = Instant.from(LocalDate.now().atStartOfDay(ZoneId.of("Europe/Rome")))

    def newReport(): Future[TokensReport] = getTokensReport().flatMap {
      case None         =>
        println("Tokens report not found, calculating from scratch")
        allTokensPaths().flatMap(updateReport(Instant.MIN, beforeThan)(TokensReport.empty))
      case Some(report) =>
        println("Tokens report found, calculating delta")
        val lastDate                     = report.lastDate()
        val afterThan                    = Instant.from(lastDate.atStartOfDay(ZoneId.of("Europe/Rome")))
        val missingDays: List[LocalDate] = datesUntilToday(lastDate)
        val prunedReport: TokensReport   = report.allButLastDate

        Future
          .traverseWithLatch(10)(missingDays)(listTokensForDay)
          .map(_.flatten)
          .flatMap(updateReport(afterThan, beforeThan)(prunedReport))
    }

    def store(data: Array[Byte]): Future[Unit] = {
      println(s"Storing report")
      fileManager
        .storeBytes(storageConfiguration.bucket, "/", storageConfiguration.tokensPartialReportFilename)(data)
        .map(_ => ())
    }

    newReport()
      .flatMap(report =>
        store(report.saveFormat.getBytes).map { _ =>
          val issueTimes: List[OffsetDateTime] = report.toList
          TokensData(issueTimes.size, issueTimes.count(_.isAfter(threeDaysAgo)), Graph.getGraphPoints(10)(issueTimes))
        }
      )
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
