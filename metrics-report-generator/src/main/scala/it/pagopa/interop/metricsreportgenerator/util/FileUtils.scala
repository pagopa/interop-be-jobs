package it.pagopa.interop.metricsreportgenerator.util

import com.typesafe.scalalogging.Logger
import it.pagopa.interop.commons.files.service.FileManager
import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier

import java.time.format.DateTimeFormatter
import scala.concurrent.Future
import java.util.UUID

final class FileUtils(val fileManager: FileManager, val dateTimeSupplier: OffsetDateTimeSupplier) {

  private val logger: Logger         = Logger(this.getClass)
  private val df: DateTimeFormatter  = DateTimeFormatter.ofPattern("yyyyMMdd")
  private val dtf: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
  def today(): String                = dateTimeSupplier.get().format(df)
  def now(): String                  = dateTimeSupplier.get().format(dtf)

  def store(config: ContainerConfiguration)(lines: Seq[String]): Future[String] = {
    val fileName: String = s"${now()}_${UUID.randomUUID()}.csv"
    logger.info(s"Storing ${lines.size} lines at ${config.container}/${config.path}/${today()}/$fileName")
    fileManager.storeBytes(config.container, today(), fileName)(lines.mkString("\n").getBytes())
  }

}
