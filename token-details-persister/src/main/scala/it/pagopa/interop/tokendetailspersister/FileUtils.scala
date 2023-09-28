package it.pagopa.interop.tokendetailspersister

import com.typesafe.scalalogging.Logger
import it.pagopa.interop.commons.files.service.FileManager
import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier
import it.pagopa.interop.commons.utils.JWTPathGenerator._

import scala.concurrent.Future

final class FileUtils(val fileManager: FileManager, val dateTimeSupplier: OffsetDateTimeSupplier) {

  private val logger: Logger = Logger(this.getClass)
  private val cPath: String  = ApplicationConfiguration.containerPath.stripMargin('/')

  def store(lines: List[String]): Future[String] = {
    val jwtPathInfo: JWTPathInfo = generateJWTPathInfo(dateTimeSupplier)

    logger.info(s"Storing ${lines.size} lines at $cPath/${jwtPathInfo.path}/${jwtPathInfo.filename}")

    // As long as each message is in json format, the file will be in ndjson format
    val content: Array[Byte] = lines.mkString("\n").getBytes()
    fileManager.storeBytes(cPath, jwtPathInfo.path, jwtPathInfo.filename)(content)
  }

}
