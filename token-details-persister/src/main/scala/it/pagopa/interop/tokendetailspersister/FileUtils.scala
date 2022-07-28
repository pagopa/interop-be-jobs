package it.pagopa.interop.tokendetailspersister

import com.typesafe.scalalogging.Logger
import it.pagopa.interop.commons.files.service.FileManager
import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import scala.concurrent.Future

final class FileUtils(val fileManager: FileManager, val dateTimeSupplier: OffsetDateTimeSupplier) {

  private val logger: Logger         = Logger(this.getClass)
  private val df: DateTimeFormatter  = DateTimeFormatter.ofPattern("yyyyMMdd")
  private val dtf: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
  private val cPath: String          = ApplicationConfiguration.containerPath.stripMargin('/')
  private val fPath: String          = ApplicationConfiguration.tokenStoragePath.stripMargin('/')

  private def fileName(now: OffsetDateTime): String = s"${now.format(dtf)}${UUID.randomUUID()}.ndjson"

  def store(lines: List[String]): Future[String] = {
    val now           = dateTimeSupplier.get
    val today         = now.format(df)
    val fName: String = fileName(now)
    logger.info(s"Storing ${lines.size} lines at $cPath/$fPath/$today/$fName")

    // As long as each message is in json format, the file will be in ndjson format
    val content: Array[Byte] = lines.mkString("\n").getBytes()
    fileManager.storeBytes(cPath, fPath)(today, fName, content)
  }

}
