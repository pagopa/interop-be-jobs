package it.pagopa.interop.metricsreportgenerator.util
import it.pagopa.interop.catalogmanagement.model.{CatalogDescriptor, CatalogItem, Draft}
import it.pagopa.interop.commons.files.service.FileManager
import it.pagopa.interop.commons.parser.{InterfaceParser, InterfaceParserUtils}
import it.pagopa.interop.commons.utils.Digester
import it.pagopa.interop.commons.utils.TypeConversions.EitherOps
import it.pagopa.interop.metricsreportgenerator.models.{FileMetricInfo, Metric}

import java.time.{OffsetDateTime, ZoneOffset}
import scala.concurrent.{ExecutionContext, Future}
object Utils {

  final val defaultActivatedAt: OffsetDateTime = OffsetDateTime.of(2022, 12, 15, 12, 0, 0, 0, ZoneOffset.UTC)

  def getMeasurableEServices(eService: CatalogItem): Boolean = eService.descriptors.exists(_.state != Draft)

  def createMetric(
                    metricFunc: (String, OffsetDateTime, FileMetricInfo) => Metric
                  )(descriptor: CatalogDescriptor)(implicit ec: ExecutionContext, fileManager: FileManager): Future[Metric] =
    for {
      stream <- fileManager.get(ApplicationConfiguration.containerPath)(descriptor.interface.get.path)
      fileMetricInfo <- getFileMetricInfo(stream.toByteArray).toFuture
    } yield metricFunc(descriptor.version, descriptor.activatedAt.getOrElse(defaultActivatedAt), fileMetricInfo)

  def getFileMetricInfo(bytes: Array[Byte]): Either[Throwable, FileMetricInfo] =
    getJsonMetricInfo(bytes) orElse getXMLMetricInfo(bytes)

  private def getJsonMetricInfo(bytes: Array[Byte]): Either[Throwable, FileMetricInfo] =
    InterfaceParser
      .parseOpenApi(bytes)
      .flatMap(json =>
        InterfaceParserUtils.getEndpoints(json).map(es => FileMetricInfo(Digester.toSha256(bytes), es.size))
      )

  private def getXMLMetricInfo(bytes: Array[Byte]): Either[Throwable, FileMetricInfo] =
    InterfaceParser
      .parseSoap(bytes)
      .flatMap(xml =>
        InterfaceParserUtils.getEndpoints(xml).map(es => FileMetricInfo(Digester.toSha256(bytes), es.size))
      )

}
