package it.pagopa.interop.metricsreportgenerator.util

import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier
import it.pagopa.interop.metricsreportgenerator.report.AgreementRecord
import spray.json.enrichAny

import java.time.format.DateTimeFormatter
import java.util.UUID
import scala.concurrent.Future

final class FileWriters(
  fileUtils: FileUtils,
  config: AgreementsConfiguration,
  dateTimeSupplier: OffsetDateTimeSupplier
) {

  val dtf: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

  def agreementsJsonWriter: Seq[AgreementRecord] => Future[String] = agreementRecords => {
    val fileName: String = s"${dateTimeSupplier.get().format(dtf)}_${UUID.randomUUID()}.ndjson"
    fileUtils.store(config.container, config.jsonStoragePath, fileName)(agreementRecords.map(_.toJson.compactPrint))
  }

  def agreementsCsvWriter: Seq[AgreementRecord] => Future[String] = agreementRecords => {
    val fileName: String = s"${dateTimeSupplier.get().format(dtf)}_${UUID.randomUUID()}.csv"
    fileUtils.store(config.container, config.csvStoragePath, fileName)(AgreementRecord.csv(agreementRecords))
  }
}
