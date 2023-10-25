package it.pagopa.interop.metricsreportgenerator.util

import it.pagopa.interop.commons.files.service.FileManager

import scala.concurrent.{ExecutionContext, Future}
import com.typesafe.scalalogging.LoggerTakingImplicit
import it.pagopa.interop.commons.logging._
import it.pagopa.interop.commons.utils.TypeConversions._
import it.pagopa.interop.metricsreportgenerator.util.Errors.InterfacePathNotFound

import scala.util.{Failure, Success}
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import it.pagopa.interop.metricsreportgenerator.util.models.Report

class S3(fileManager: FileManager, config: Configuration)(implicit
  logger: LoggerTakingImplicit[ContextFieldsToLog],
  context: ContextFieldsToLog,
  ec: ExecutionContext
) {

  val saveAgreementsReport: Array[Byte] => Future[Unit]        = store(s"agreements-${config.environment}.csv")
  val saveActiveDescriptorsReport: Array[Byte] => Future[Unit] = store(s"active-descriptors-${config.environment}.csv")
  val saveTokensReport: Array[Byte] => Future[Unit]            = store(s"tokens-${config.environment}.csv")

  def readTokensReport(): Future[Option[Report]] = {
    val path: String = config.metric.basePath.stripPrefix("/").stripSuffix("/") match {
      case "" => s"tokens-${config.environment}.csv"
      case x  => s"$x/tokens-${config.environment}.csv"
    }

    logger.info(s"Searching token report at ${config.metric.bucket} ${path}")
    fileManager
      .getFile(config.metric.bucket)(path)
      .transformWith {
        case Failure(_)     => Future.successful(Option.empty[Report])
        case Success(bytes) => Report.from(bytes).map(Option(_)).toFuture
      }
  }

  def listTokensForDay(date: LocalDate): Future[List[String]] = {
    val path = config.tokens.basePath
      .stripPrefix("/")
      .stripSuffix("/")
      .concat("/")
      .concat(date.format(DateTimeFormatter.BASIC_ISO_DATE))

    logger.debug(s"Getting tokens in ${config.tokens.bucket}/${path}")

    fileManager.listFiles(config.tokens.bucket)(path)
  }

  def listAllTokens(): Future[List[String]] = fileManager.listFiles(config.tokens.bucket)(config.tokens.basePath)

  def getToken(path: String): Future[Array[Byte]] = fileManager.getFile(config.tokens.bucket)(path)

  def getInterfaceDocument(path: Option[String]): Future[Array[Byte]] =
    path.fold(Future.failed[Array[Byte]](InterfacePathNotFound))(p => fileManager.getFile(config.interface.bucket)(p))

  private def renderPath(fileName: String): String = List(config.metric.bucket, config.metric.basePath, fileName)
    .map(_.stripMargin('/'))
    .mkString("/")
    .replaceAll("/+", "/")

  private def store(fileName: String)(data: Array[Byte]): Future[Unit] = {
    logger.info(s"Storing report at ${renderPath(fileName)}")
    fileManager.storeBytes(config.metric.bucket, config.metric.basePath.stripPrefix("/"), fileName)(data).map(_ => ())
  }

}
