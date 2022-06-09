package it.pagopa.interop.tokenreader.utils

import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.server.directives.FileInfo
import it.pagopa.interop.commons.files.service.FileManager
import it.pagopa.interop.tokenreader.system.ApplicationConfiguration
import org.slf4j.{Logger, LoggerFactory}

import java.io.{BufferedWriter, File, FileWriter}
import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Try, Using}

final class FileUtils(val fileManager: FileManager)(implicit ec: ExecutionContext) {

  val logger: Logger         = LoggerFactory.getLogger(this.getClass)
  val dtf: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

  def writeOnFile(lines: List[String]): Future[Unit] = {

    val tempFile = File.createTempFile(LocalDateTime.now().format(dtf), ".txt")
    logger.info("{} created.", tempFile.getName)

    val datePath: String = LocalDate.now.format(DateTimeFormatter.ISO_DATE)
    val filePath: String = s"${ApplicationConfiguration.tokenStoragePath}/$datePath"

    val writing = for {
      _ <- Future { writeFile(tempFile, lines) }
      fileParts = (FileInfo("file", tempFile.getName, ContentTypes.NoContentType), tempFile)
      path <- fileManager.store(ApplicationConfiguration.storageContainer, filePath)(UUID.randomUUID(), fileParts)
      _ = logger.info("File stored at {}", path)
    } yield ()

    writing.andThen { case _ =>
      deleteFile(tempFile) match {
        case Left(x)      => logger.error("Error while deleting {}", tempFile.getName, x)
        case Right(state) => logger.info("Deleting operation of {} returned this state = {}", tempFile.getName, state)
      }
    }
  }

  private def deleteFile(file: File): Either[Throwable, Boolean] = {
    if (file.exists()) Try { file.delete() }.toEither
    else {
      logger.warn(s"File {} does not exist ", file.getName)
      Right(true)
    }

  }

  private def writeFile(file: File, lines: Seq[String]): Unit = {
    val bw = new BufferedWriter(new FileWriter(file))
    Using(bw) { writer =>
      for (line <- lines) {
        writer.write(line)
      }
      logger.info(s"${lines.size} lines written on ${file.getName}")
    }
  }
}
