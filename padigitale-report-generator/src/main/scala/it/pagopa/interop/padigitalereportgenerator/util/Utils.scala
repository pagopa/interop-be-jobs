package it.pagopa.interop.padigitalereportgenerator.util

import it.pagopa.interop.commons.logging._
import com.typesafe.scalalogging.LoggerTakingImplicit
import it.pagopa.interop.catalogmanagement.model.{CatalogDescriptor, CatalogItem, Draft}
import it.pagopa.interop.commons.cqrs.service.ReadModelService
import it.pagopa.interop.commons.parser.{InterfaceParser, InterfaceParserUtils}
import it.pagopa.interop.commons.utils.TypeConversions.{EitherOps, OptionOps}
import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier
import it.pagopa.interop.padigitalereportgenerator.report.{FileExtractedMetrics, Metric, MetricGeneratorSeed}

import scala.concurrent.{ExecutionContext, Future}

object Utils {

  final val maxLimit = 100

  def hasMeasurableEServices(eService: CatalogItem): Boolean = eService.descriptors.exists(_.state != Draft)

  def retrieveAllEServices(
    eServicesRetriever: (Int, Int) => Future[Seq[CatalogItem]],
    offset: Int,
    acc: Seq[CatalogItem]
  )(implicit
    logger: LoggerTakingImplicit[ContextFieldsToLog],
    context: List[(String, String)],
    ex: ExecutionContext
  ): Future[Seq[CatalogItem]] =
    eServicesRetriever(offset, maxLimit).flatMap(eServices =>
      if (eServices.isEmpty) {
        logger.info(s"EServices load completed size ${acc.size}")
        Future.successful(acc)
      } else retrieveAllEServices(eServicesRetriever, offset + maxLimit, acc ++ eServices)
    )

  def createMetrics(implicit
    readModelService: ReadModelService,
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
        timestamp = dateTimeSupplier.get()
      )
      measurableDescriptors = eService.descriptors.filter(_.state != Draft)
      metrics <- Future.traverse(measurableDescriptors)(createMetric(metricGenerator))
    } yield metrics

  private def createMetric(
    metricGenerator: MetricGeneratorSeed => Metric
  )(descriptor: CatalogDescriptor)(implicit ec: ExecutionContext): Future[Metric] =
    for {
      fileExtractedMetrics <- getFileExtractedMetrics(descriptor.interface.get.checksum).toFuture
      publishedAt          <- descriptor.publishedAt.toFuture(Error.MissingActivationTimestamp(descriptor.id))
    } yield metricGenerator(
      MetricGeneratorSeed(
        descriptor.version,
        descriptor.state.toString,
        descriptor.createdAt,
        publishedAt,
        fileExtractedMetrics
      )
    )

  def getFileExtractedMetrics(fingerprint: String): Either[Throwable, FileExtractedMetrics] =
    getOpenApiExtractedMetrics(fingerprint) orElse getWSDLExtractedMetrics(fingerprint)

  private def getOpenApiExtractedMetrics(fingerprint: String): Either[Throwable, FileExtractedMetrics] =
    InterfaceParser
      .parseOpenApi(fingerprint.getBytes())
      .flatMap(json => InterfaceParserUtils.getEndpoints(json).map(es => FileExtractedMetrics(fingerprint, es.size)))

  private def getWSDLExtractedMetrics(fingerprint: String): Either[Throwable, FileExtractedMetrics] =
    InterfaceParser
      .parseWSDL(fingerprint.getBytes())
      .flatMap(xml => InterfaceParserUtils.getEndpoints(xml).map(es => FileExtractedMetrics(fingerprint, es.size)))

}
