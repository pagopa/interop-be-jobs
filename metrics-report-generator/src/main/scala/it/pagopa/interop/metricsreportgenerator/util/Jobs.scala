package it.pagopa.interop.metricsreportgenerator.util

import cats.syntax.all._
import scala.concurrent.Future
import java.time.OffsetDateTime
import it.pagopa.interop.commons.files.service.FileManager
import spray.json._
import it.pagopa.interop.commons.utils.TypeConversions._
import it.pagopa.interop.commons.utils.errors.GenericComponentErrors.GenericError
import it.pagopa.interop.commons.cqrs.service.ReadModelService
import scala.concurrent.ExecutionContext
import it.pagopa.interop.metricsreportgenerator.util.models.Descriptor
import cats.Functor
import com.typesafe.scalalogging.LoggerTakingImplicit
import it.pagopa.interop.commons.logging._
import scala.util.{Try, Failure, Success}
import scala.collection.immutable.SortedMap

class Jobs(config: Configuration, fileManager: FileManager, readModel: ReadModelService)(implicit
  logger: LoggerTakingImplicit[ContextFieldsToLog],
  context: ContextFieldsToLog
) {

  def getTokensData(implicit ec: ExecutionContext): Future[List[String]] = {

    logger.info("Gathering tokens information")

    def allTokensPaths(): Future[List[String]] = fileManager.listFiles(config.tokens.bucket)(config.tokens.basePath)

    // There's no point in doing this operation in a Future stack as it isn't I/O nor CPU bound
    def extractDataFromToken(token: String): Try[String] = Try {
      val fields: Map[String, JsValue] = token.parseJson.asJsObject.fields
      val aId: String                  = fields
        .get("agreementId")
        .collect { case JsString(x) => x }
        .getOrElse(throw GenericError("Missing agreementId field in token"))
      val pId: String                  = fields
        .get("purposeId")
        .collect { case JsString(x) => x }
        .getOrElse(throw GenericError("Missing purposeId field in token"))
      val dateL: Long                  = fields
        .get("issuedAt")
        .collect { case JsNumber(x) => x.toLong }
        .getOrElse(throw GenericError("Missing issuedAt field in token"))
      val time: OffsetDateTime         = dateL.toOffsetDateTime.get

      s""""$aId","$pId","${time.getYear}","${time.getMonthValue}","${time.getDayOfMonth}""""
    }

    // UGLY but performance and memory efficient
    def createReport(tokens: List[String]): Try[Map[String, Int]] = {
      val map = new scala.collection.mutable.HashMap[String, Int]
      for (token <- tokens) {
        extractDataFromToken(token) match {
          case Success(rh) =>
            map.updateWith(rh) {
              case Some(x) => Some(x + 1)
              case None    => Some(1)
            }
          case Failure(ex) => return Failure(ex)
        }
      }
      Success(map.toMap)
    }

    def getTokensFromFile(path: String): Future[List[String]] = fileManager
      .getFile(config.tokens.bucket)(path)
      .map(bs => new String(bs).split('\n').toList)

    def createSingleReportFromFile(path: String): Future[Map[String, Int]] =
      getTokensFromFile(path).flatMap(createReport(_).toFuture)

    for {
      paths   <- allTokensPaths()
      reports <- Future.traverseWithLatch(10)(paths)(createSingleReportFromFile)
      reportAsMap = reports.foldLeft(SortedMap.empty[String, Int])(_.concat(_))
      report      = reportAsMap.map { case (k, v) => s"""$k,"$v"""" }.toList
    } yield "agreementId,purposeId,year,month,day,tokencount" :: report
  }

  def getAgreementRecord(implicit ec: ExecutionContext): Future[List[String]] = {
    logger.info("Gathering Agreements Information")

    ReadModelQueries
      .getAllActiveAgreements(config.collections, readModel)
      .zip(ReadModelQueries.getAllPurposes(config.collections, readModel))
      .map { case (agreements, purposes) =>
        val header = "eserviceId,eservice,producer,consumer,agreementId,purposes,purposeIds"
        header :: agreements.toList.map { a =>
          val (purposeIds, purposeNames): (Seq[String], Seq[String]) = purposes
            .filter(p => p.consumerId == a.consumerId && p.eserviceId == a.eserviceId)
            .map(p => (p.purposeId, p.name))
            .separate
          List(
            a.eserviceId,
            a.eservice,
            a.producer,
            a.consumer,
            a.agreementId,
            purposeNames.mkString("§"),
            purposeIds.mkString("§")
          ).map(s => s"\"$s\"").mkString(",")
        }
      }
  }

  def getDescriptorsRecord(implicit ec: ExecutionContext): Future[List[String]] = {
    logger.info("Gathering Descriptors Information")

    val header: String                              = "name,createdAt,producerId,producer,descriptorId,state"
    val asCsvRow: Descriptor => String              = (d: Descriptor) =>
      s""""${d.name}","${d.createdAt}","${d.producerId}","${d.producer}","${d.descriptorId}","${d.state}""""
    val asCsvRows: List[Descriptor] => List[String] = Functor[List].lift(asCsvRow)
    val addHeader: List[String] => List[String]     = header :: _

    val asCsv: List[Descriptor] => List[String] = asCsvRows.andThen(addHeader)

    ReadModelQueries
      .getAllDescriptors(config.collections, readModel)
      .map(_.filter(_.isActive).toList)
      .map(asCsv)
  }

  def store(fileName: String, lines: List[String])(implicit ec: ExecutionContext): Future[List[String]] = {
    val path: String = List(config.storage.bucket, config.storage.basePath, fileName)
      .map(_.stripMargin('/'))
      .mkString("/")
      .replaceAll("/+", "/")

    logger.info(s"Storing ${lines.size} lines at $path")
    fileManager
      .storeBytes(config.storage.bucket, config.storage.basePath.stripPrefix("/"), fileName)(
        lines.mkString("\n").getBytes()
      )
      .map(_ => lines)
  }

}
