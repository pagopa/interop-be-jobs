package it.pagopa.interop.metricsreportgenerator

import cats.implicits._
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import io.circe.syntax._
import it.pagopa.interop.catalogmanagement.model.CatalogItem
import it.pagopa.interop.commons.files.service.FileManager
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.TypeConversions.EitherOps
import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier
import it.pagopa.interop.metricsreportgenerator.models.Metric
import it.pagopa.interop.metricsreportgenerator.repository.impl.{CatalogRepositoryImpl, TenantRepositoryImpl}
import it.pagopa.interop.metricsreportgenerator.repository.{CatalogRepository, TenantRepository}
import it.pagopa.interop.metricsreportgenerator.util.{ApplicationConfiguration, FileUtils}
import it.pagopa.interop.metricsreportgenerator.util.Utils.{createMetric, getMeasurableEServices}
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.connection.NettyStreamFactoryFactory
import org.mongodb.scala.{ConnectionString, MongoClient, MongoClientSettings}

import java.util.concurrent.{ExecutorService, Executors}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}
object Main extends App {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  implicit val contexts: Seq[(String, String)] = Seq.empty

  val blockingThreadPool: ExecutorService = Executors.newFixedThreadPool(Runtime.getRuntime.availableProcessors())
  implicit val blockingEC: ExecutionContextExecutor = ExecutionContext.fromExecutor(blockingThreadPool)

  implicit val client: MongoClient = MongoClient(
    MongoClientSettings
      .builder()
      .applyConnectionString(new ConnectionString(ApplicationConfiguration.databaseURL))
      .codecRegistry(DEFAULT_CODEC_REGISTRY)
      .streamFactoryFactory(NettyStreamFactoryFactory())
      .build()
  )

  logger.info("Starting metrics report generator job")

  implicit val fileManager: FileManager = FileManager.get(FileManager.S3)(blockingEC)

  val catalogRepository: CatalogRepository     = new CatalogRepositoryImpl(client)
  val tenantRepository: TenantRepository       = new TenantRepositoryImpl(client)
  val dateTimeSupplier: OffsetDateTimeSupplier = OffsetDateTimeSupplier
  val fileUtils                                = new FileUtils(fileManager, dateTimeSupplier)

  def createMetrics: CatalogItem => Future[List[Metric]] = eService =>
    for {
      producer <- tenantRepository.getTenant(eService.producerId)
      metricFunc = Metric.create(
        originId = producer.externalId.value,
        origin = producer.externalId.origin,
        name = eService.name,
        technology = eService.technology.toString
      )(dateTimeSupplier)
      metrics <- Future.traverse(eService.descriptors)(createMetric(metricFunc))
    } yield metrics.toList

  val execution: Future[String] = for {
    eServices <- catalogRepository.getEServices.flatMap(_.sequence.toFuture)
    measurable = eServices.toList.filter(getMeasurableEServices)
    metrics <- measurable.flatTraverse(createMetrics)
    records = metrics.map(_.asJson.noSpaces)
    result <- fileUtils.store(records)
  } yield result

  execution
    .recover { ex =>
      logger.error("There was an error while running the job", ex)
    }(global)
    .andThen(_ => blockingThreadPool.shutdown())

  Await.result(execution, Duration.Inf)
  logger.info("Completed metrics report generator job")

}
