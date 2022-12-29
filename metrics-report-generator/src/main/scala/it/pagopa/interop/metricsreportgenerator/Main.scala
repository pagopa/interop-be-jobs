package it.pagopa.interop.metricsreportgenerator

import cats.implicits._
import com.typesafe.scalalogging.Logger
import io.circe.syntax._
import it.pagopa.interop.catalogmanagement.model.{CatalogItem, Draft}
import it.pagopa.interop.commons.cqrs.service.ReadModelService
import it.pagopa.interop.commons.files.service.FileManager
import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier
import it.pagopa.interop.metricsreportgenerator.models.{ActiveAgreement, Metric, Purpose}
import it.pagopa.interop.metricsreportgenerator.util.Utils.{createMetric, hasMeasurableEServices}
import it.pagopa.interop.metricsreportgenerator.util.{ApplicationConfiguration, FileUtils, ReadModelQueries}
import spray.json.enrichAny

import java.util.concurrent.{ExecutorService, Executors}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}

object Main extends App {

  private val logger: Logger = Logger(this.getClass)

  logger.info("Starting metrics report generator job")

  final val maxLimit = 10

  val blockingThreadPool: ExecutorService = Executors.newFixedThreadPool(Runtime.getRuntime.availableProcessors())
  implicit val blockingEC: ExecutionContextExecutor = ExecutionContext.fromExecutor(blockingThreadPool)

  implicit val fileManager: FileManager = FileManager.get(FileManager.S3)(blockingEC)

  implicit val readModelService: ReadModelService = new ReadModelService(ApplicationConfiguration.readModelConfig)

  val dateTimeSupplier: OffsetDateTimeSupplier = OffsetDateTimeSupplier

  val fileUtils = new FileUtils(fileManager, dateTimeSupplier)

  def execution: Future[Unit] = for {
    eServices <- retrieveAllEServices(ReadModelQueries.getEServices, 0, Seq.empty)
    measurableEServices = eServices.toList.filter(hasMeasurableEServices)
    metrics <- measurableEServices.flatTraverse(createMetrics)
    records = metrics.map(_.asJson.noSpaces)
    activeAgreements <- retrieveAllActiveAgreements(ReadModelQueries.getActiveAgreements, 0, Seq.empty)
    purposes         <- retrieveAllPurposes(ReadModelQueries.getPurposes, 0, Seq.empty)
    _ = activeAgreements.map(_.toJson.compactPrint).foreach(println)
    _ = purposes.map(_.toJson.compactPrint).foreach(println)
    _ <- fileUtils.store(records)
  } yield ()

  execution
    .recover { ex =>
      logger.error("There was an error while running the job", ex)
    }(global)
    .andThen(_ => blockingThreadPool.shutdown())

  Await.result(execution, Duration.Inf)
  logger.info("Completed metrics report generator job")

  def retrieveAllEServices(
    eServicesRetriever: (Int, Int) => Future[Seq[CatalogItem]],
    offset: Int,
    acc: Seq[CatalogItem]
  ): Future[Seq[CatalogItem]] =
    eServicesRetriever(offset, maxLimit).flatMap(eServices =>
      if (eServices.isEmpty) {
        logger.info(s"EServices load completed size ${acc.size}")
        Future.successful(acc)
      } else retrieveAllEServices(eServicesRetriever, offset + maxLimit, acc ++ eServices)
    )

  def createMetrics: CatalogItem => Future[List[Metric]] = eService =>
    for {
      producer <- ReadModelQueries.getTenant(eService.producerId)
      metricGenerator       = Metric.generator(
        originId = producer.externalId.value,
        origin = producer.externalId.origin,
        name = eService.name,
        eServiceId = eService.id.toString,
        technology = eService.technology.toString,
        createdAt = dateTimeSupplier.get()
      )
      measurableDescriptors = eService.descriptors.filter(_.state != Draft)
      metrics <- Future.traverse(measurableDescriptors)(createMetric(metricGenerator))
    } yield metrics.toList

  def retrieveAllActiveAgreements(
    agreementsRetriever: (Int, Int) => Future[Seq[ActiveAgreement]],
    offset: Int,
    acc: Seq[ActiveAgreement]
  ): Future[Seq[ActiveAgreement]] =
    agreementsRetriever(offset, maxLimit).flatMap(agreements =>
      if (agreements.isEmpty) {
        logger.info(s"Active agreements load completed size ${acc.size}")
        Future.successful(acc)
      } else retrieveAllActiveAgreements(agreementsRetriever, offset + maxLimit, acc ++ agreements)
    )

  def retrieveAllPurposes(
    purposesRetriever: (Int, Int) => Future[Seq[Purpose]],
    offset: Int,
    acc: Seq[Purpose]
  ): Future[Seq[Purpose]] =
    purposesRetriever(offset, maxLimit).flatMap(purposes =>
      if (purposes.isEmpty) {
        logger.info(s"Purposes load completed size ${acc.size}")
        Future.successful(acc)
      } else retrieveAllPurposes(purposesRetriever, offset + maxLimit, acc ++ purposes)
    )

}
