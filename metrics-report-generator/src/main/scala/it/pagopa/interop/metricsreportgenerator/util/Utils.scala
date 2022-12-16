package it.pagopa.interop.metricsreportgenerator.util
import it.pagopa.interop.catalogmanagement.model.{CatalogDescriptor, CatalogItem, Draft}
import it.pagopa.interop.commons.files.service.FileManager
import it.pagopa.interop.commons.parser.{InterfaceParser, InterfaceParserUtils}
import it.pagopa.interop.commons.utils.Digester
import it.pagopa.interop.commons.utils.TypeConversions.EitherOps
import it.pagopa.interop.metricsreportgenerator.models.{FileExtractedMetrics, Metric}

import java.time.{OffsetDateTime, ZoneOffset}
import scala.concurrent.{ExecutionContext, Future}
object Utils {

  final val defaultActivatedAt: OffsetDateTime = OffsetDateTime.of(2022, 12, 15, 12, 0, 0, 0, ZoneOffset.UTC)

  def getMeasurableEServices(eService: CatalogItem): Boolean = eService.descriptors.exists(_.state != Draft)

  def createMetric(
    metricGenerator: (String, OffsetDateTime, FileExtractedMetrics) => Metric
  )(descriptor: CatalogDescriptor)(implicit ec: ExecutionContext, fileManager: FileManager): Future[Metric] =
    for {
      stream <- fileManager.get(ApplicationConfiguration.interfacesContainer)(descriptor.interface.get.path)
      fileExtractedMetrics <- getFileExtractedMetrics(stream.toByteArray).toFuture
    } yield metricGenerator(descriptor.version, defaultActivatedAt, fileExtractedMetrics)

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
