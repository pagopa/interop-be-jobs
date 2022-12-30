package it.pagopa.interop.metricsreportgenerator.util

import io.circe.syntax._
import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier
import it.pagopa.interop.metricsreportgenerator.report.{AgreementRecord, Metric}
import spray.json.enrichAny

import java.time.format.DateTimeFormatter
import java.util.UUID
import scala.concurrent.Future

final class FileWriters(fileUtils: FileUtils, dateTimeSupplier: OffsetDateTimeSupplier) {

  val dtf: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

  def paDigitaleWriter: Seq[Metric] => Future[String] = metrics => {
    val fileName: String = s"${dateTimeSupplier.get().format(dtf)}_${UUID.randomUUID()}.ndjson"
    fileUtils.store(
      ApplicationConfiguration.paDigitaleContainer,
      ApplicationConfiguration.paDigitaleStoragePath,
      fileName
    )(metrics.map(_.asJson.noSpaces))
  }

  def agreementsJsonWriter: Seq[AgreementRecord] => Future[String] = agreementRecords => {
    val fileName: String = s"${dateTimeSupplier.get().format(dtf)}_${UUID.randomUUID()}.ndjson"
    fileUtils.store(
      ApplicationConfiguration.agreementsContainer,
      s"${ApplicationConfiguration.agreementsJsonStoragePath}",
      fileName
    )(agreementRecords.map(_.toJson.compactPrint))
  }

  def agreementsCsvWriter: Seq[AgreementRecord] => Future[String] = agreementRecords => {
    val fileName: String = s"${dateTimeSupplier.get().format(dtf)}_${UUID.randomUUID()}.csv"
    fileUtils.store(
      ApplicationConfiguration.agreementsContainer,
      s"${ApplicationConfiguration.agreementsCsvStoragePath}",
      fileName
    )(AgreementRecord.csv(agreementRecords))
  }
}
