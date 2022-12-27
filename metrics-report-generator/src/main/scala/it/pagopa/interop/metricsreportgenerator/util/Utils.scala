package it.pagopa.interop.metricsreportgenerator.util
import it.pagopa.interop.catalogmanagement.model.{CatalogDescriptor, CatalogItem, Draft}
import it.pagopa.interop.commons.files.service.FileManager
import it.pagopa.interop.commons.parser.{InterfaceParser, InterfaceParserUtils}
import it.pagopa.interop.commons.utils.Digester
import it.pagopa.interop.commons.utils.TypeConversions.{EitherOps, OptionOps}
import it.pagopa.interop.metricsreportgenerator.models.{FileExtractedMetrics, Metric}

import java.time.OffsetDateTime
import scala.concurrent.{ExecutionContext, Future}
object Utils {

  def hasMeasurableEServices(eService: CatalogItem): Boolean = eService.descriptors.exists(_.state != Draft)

  def createMetric(
    metricGenerator: (String, OffsetDateTime, FileExtractedMetrics) => Metric
  )(descriptor: CatalogDescriptor)(implicit ec: ExecutionContext, fileManager: FileManager): Future[Metric] =
    for {
      stream <- fileManager.get(ApplicationConfiguration.interfacesContainer)(descriptor.interface.get.path)
      fileExtractedMetrics <- getFileExtractedMetrics(stream.toByteArray).toFuture
      activatedAt          <- descriptor.activatedAt.toFuture(Error.MissingActivationTimestamp(descriptor.id))
    } yield metricGenerator(descriptor.version, activatedAt, fileExtractedMetrics)

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
