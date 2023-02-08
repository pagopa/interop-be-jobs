package it.pagopa.interop.dashboardmetricsreportgenerator.util

import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier
import it.pagopa.interop.dashboardmetricsreportgenerator.report.AgreementRecord
import spray.json.enrichAny

import java.time.format.DateTimeFormatter
import java.util.UUID
import scala.concurrent.Future

final class FileWriters(fileUtils: FileUtils, dateTimeSupplier: OffsetDateTimeSupplier) {

  val dtf: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

  def agreementsJsonWriter: Seq[AgreementRecord] => Future[String] = agreementRecords => {
    val fileName: String = s"${dateTimeSupplier.get().format(dtf)}_${UUID.randomUUID()}.ndjson"
    fileUtils.store(
      ApplicationConfiguration.agreementsContainer,
      ApplicationConfiguration.agreementsJsonStoragePath,
      fileName
    )(agreementRecords.map(_.toJson.compactPrint))
  }

  def agreementsCsvWriter: Seq[AgreementRecord] => Future[String] = agreementRecords => {
    val fileName: String = s"${dateTimeSupplier.get().format(dtf)}_${UUID.randomUUID()}.csv"
    fileUtils.store(
      ApplicationConfiguration.agreementsContainer,
      ApplicationConfiguration.agreementsCsvStoragePath,
      fileName
    )(AgreementRecord.csv(agreementRecords))
  }
}
