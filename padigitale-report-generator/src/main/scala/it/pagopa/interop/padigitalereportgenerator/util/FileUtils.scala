package it.pagopa.interop.padigitalereportgenerator.util

import com.typesafe.scalalogging.Logger
import it.pagopa.interop.commons.files.service.FileManager
import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import scala.concurrent.Future

final class FileUtils(val fileManager: FileManager, val dateTimeSupplier: OffsetDateTimeSupplier) {

  private val logger: Logger        = Logger(this.getClass)
  private val df: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

  def store(containerPath: String, filePath: String, fileName: String)(lines: Seq[String]): Future[String] = {
    val now: OffsetDateTime  = dateTimeSupplier.get()
    val today: String        = now.format(df)
    logger.info(s"Storing ${lines.size} lines at $containerPath/$today/$fileName")
    val content: Array[Byte] = lines.mkString("\n").getBytes()
    fileManager.storeBytes(containerPath, filePath)(today, fileName, content)
  }

}
