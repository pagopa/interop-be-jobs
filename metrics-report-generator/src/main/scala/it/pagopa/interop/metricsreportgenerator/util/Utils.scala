package it.pagopa.interop.metricsreportgenerator.util
import com.typesafe.scalalogging.Logger
import it.pagopa.interop.catalogmanagement.model.{CatalogDescriptor, CatalogItem, Draft}
import it.pagopa.interop.commons.cqrs.service.ReadModelService
import it.pagopa.interop.commons.files.service.FileManager
import it.pagopa.interop.commons.parser.{InterfaceParser, InterfaceParserUtils}
import it.pagopa.interop.commons.utils.Digester
import it.pagopa.interop.commons.utils.TypeConversions.{EitherOps, OptionOps}
import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier
import it.pagopa.interop.metricsreportgenerator.models.{Agreement, Purpose}
import it.pagopa.interop.metricsreportgenerator.report.{FileExtractedMetrics, Metric}

import java.time.OffsetDateTime
import scala.concurrent.{ExecutionContext, Future}

object Utils {

  final val maxLimit = 100

  def hasMeasurableEServices(eService: CatalogItem): Boolean = eService.descriptors.exists(_.state != Draft)

  def retrieveAllEServices(
    eServicesRetriever: (Int, Int) => Future[Seq[CatalogItem]],
    offset: Int,
    acc: Seq[CatalogItem]
  )(implicit logger: Logger, ex: ExecutionContext): Future[Seq[CatalogItem]] =
    eServicesRetriever(offset, maxLimit).flatMap(eServices =>
      if (eServices.isEmpty) {
        logger.info(s"EServices load completed size ${acc.size}")
        Future.successful(acc)
      } else retrieveAllEServices(eServicesRetriever, offset + maxLimit, acc ++ eServices)
    )

  def createMetrics(implicit
    readModelService: ReadModelService,
    fileManager: FileManager,
    dateTimeSupplier: OffsetDateTimeSupplier,
    ex: ExecutionContext
  ): CatalogItem => Future[Seq[Metric]] = eService =>
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
    } yield metrics

  private def createMetric(
    metricGenerator: (String, OffsetDateTime, FileExtractedMetrics) => Metric
  )(descriptor: CatalogDescriptor)(implicit ec: ExecutionContext, fileManager: FileManager): Future[Metric] =
    for {
      stream <- fileManager.get(ApplicationConfiguration.interfacesContainer)(descriptor.interface.get.path)
      fileExtractedMetrics <- getFileExtractedMetrics(stream.toByteArray).toFuture
      activatedAt          <- descriptor.activatedAt.toFuture(Error.MissingActivationTimestamp(descriptor.id))
    } yield metricGenerator(descriptor.version, activatedAt, fileExtractedMetrics)

  def retrieveAllActiveAgreements(
    agreementsRetriever: (Int, Int) => Future[Seq[Agreement]],
    offset: Int,
    acc: Seq[Agreement]
  )(implicit logger: Logger, ex: ExecutionContext): Future[Seq[Agreement]] =
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
  )(implicit logger: Logger, ex: ExecutionContext): Future[Seq[Purpose]] =
    purposesRetriever(offset, maxLimit).flatMap(purposes =>
      if (purposes.isEmpty) {
        logger.info(s"Purposes load completed size ${acc.size}")
        Future.successful(acc)
      } else retrieveAllPurposes(purposesRetriever, offset + maxLimit, acc ++ purposes)
    )

  def getFileExtractedMetrics(bytes: Array[Byte]): Either[Throwable, FileExtractedMetrics] =
    getOpenApiExtractedMetrics(bytes) orElse getWSDLExtractedMetrics(bytes)

  private def getOpenApiExtractedMetrics(bytes: Array[Byte]): Either[Throwable, FileExtractedMetrics] =
    InterfaceParser
      .parseOpenApi(bytes)
      .flatMap(json =>
        InterfaceParserUtils.getEndpoints(json).map(es => FileExtractedMetrics(Digester.toSha256(bytes), es.size))
      )

  private def getWSDLExtractedMetrics(bytes: Array[Byte]): Either[Throwable, FileExtractedMetrics] =
    InterfaceParser
      .parseWSDL(bytes)
      .flatMap(xml =>
        InterfaceParserUtils.getEndpoints(xml).map(es => FileExtractedMetrics(Digester.toSha256(bytes), es.size))
      )

}
