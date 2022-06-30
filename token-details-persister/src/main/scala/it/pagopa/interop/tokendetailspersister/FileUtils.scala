package it.pagopa.interop.tokendetailspersister

import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.server.directives.FileInfo
import it.pagopa.interop.commons.files.service.FileManager
import it.pagopa.interop.tokendetailspersister.ApplicationConfiguration
import org.slf4j.{Logger, LoggerFactory}

import cats.implicits._
import java.io.{BufferedWriter, File, FileWriter}
import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Try, Using}

final class FileUtils(val fileManager: FileManager) {

  private val logger: Logger         = LoggerFactory.getLogger(this.getClass)
  private val dtf: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
  private val cPath: String          = ApplicationConfiguration.containerPath.stripMargin('/')
  private val fPath: String          = ApplicationConfiguration.tokenStoragePath.stripMargin('/')

  private def fileName(): String = s"${LocalDateTime.now().format(dtf)}.ndjson"

  def store(lines: List[String]): Future[String] = {
    val fName: String    = fileName()
    val resourceId: UUID = UUID.randomUUID()
    logger.info(s"Storing ${lines.size} lines at $cPath/$fPath/${resourceId.toString}/$fName")

    // As long as each message is in json format, the file will be in ndjson format
    val content: Array[Byte] = lines.mkString("\n").getBytes()
    fileManager.storeBytes(cPath, fPath)(resourceId, fName, content)
  }

}
