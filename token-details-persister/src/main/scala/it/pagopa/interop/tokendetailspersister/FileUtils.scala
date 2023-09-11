package it.pagopa.interop.tokendetailspersister

import com.typesafe.scalalogging.Logger
import it.pagopa.interop.commons.files.service.FileManager
import it.pagopa.interop.commons.files.service.impl.S3ManagerImpl
import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier

import scala.concurrent.Future

final class FileUtils(val fileManager: FileManager, val dateTimeSupplier: OffsetDateTimeSupplier) {

  private val logger: Logger = Logger(this.getClass)
  private val cPath: String  = ApplicationConfiguration.containerPath.stripMargin('/')
  private val fPath: String  = ApplicationConfiguration.tokenStoragePath.stripMargin('/')

  def store(lines: List[String]): Future[String] = {
    val jwtPath: String = fileManager.asInstanceOf[S3ManagerImpl].createJWTPath()

    logger.info(s"Storing ${lines.size} lines at $cPath/$fPath/$jwtPath")

    // As long as each message is in json format, the file will be in ndjson format
    val content: Array[Byte] = lines.mkString("\n").getBytes()
    fileManager.storeBytes(cPath, fPath, jwtPath)(content)
  }

}
