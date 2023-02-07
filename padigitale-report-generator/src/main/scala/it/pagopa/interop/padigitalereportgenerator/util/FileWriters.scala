package it.pagopa.interop.padigitalereportgenerator.util

import io.circe.syntax._
import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier
import it.pagopa.interop.padigitalereportgenerator.report.Metric

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
}
