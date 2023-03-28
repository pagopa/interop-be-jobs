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

class Jobs(config: Configuration, fileManager: FileManager, readModel: ReadModelService)(implicit
  logger: LoggerTakingImplicit[ContextFieldsToLog],
  context: ContextFieldsToLog
) {

  def getTokensData(implicit ec: ExecutionContext): Future[List[String]] = {

    logger.info("Gathering tokens information")

    def allTokensPaths(): Future[List[String]] = fileManager.listFiles(config.tokens.bucket)(config.tokens.basePath)

    def getTokens(path: String): Future[List[String]] = fileManager
      .getFile(config.tokens.bucket)(path)
      .map(bs => new String(bs).split('\n').toList)

    def getTokenInfo(token: String): Future[(String, String, OffsetDateTime)] = for {
      fields <- Future(token.parseJson.asJsObject.fields)
      aId    <- fields
        .get("agreementId")
        .collect { case JsString(x) => x }
        .toFuture(GenericError("Missing agreementId field in token"))
      pId    <- fields
        .get("purposeId")
        .collect { case JsString(x) => x }
        .toFuture(GenericError("Missing purposeId field in token"))
      dateL  <- fields
        .get("issuedAt")
        .collect { case JsNumber(x) => x.toLong }
        .toFuture(GenericError("Missing issuedAt field in token"))
      date   <- dateL.toOffsetDateTime.toFuture
    } yield (aId, pId, date)

    def allTokenInfo(): Future[List[(String, String, OffsetDateTime)]] = allTokensPaths().flatMap(paths =>
      Future
        .traverseWithLatch(config.collections.maxParallelism)(paths)(path =>
          getTokens(path).flatMap(Future.traverse(_)(getTokenInfo))
        )
        .map(_.flatten)
    )

    allTokenInfo().map(
      _.map { case (aId, pId, time) => (aId, pId, time.getYear, time.getMonthValue, time.getDayOfMonth) }
        .groupMapReduce(identity)(_ => 1)(_ + _) // count occurrences
        .toList
        .sortBy { case ((aId, pId, y, m, d), _) => (aId, pId, y, m, d) }
        // to make it prettier you should let me use Shapeless or Scala 3
        .map { case ((aId, pId, y, m, d), c) => s""""$aId","$pId","$y","$m","$d","$c"""" }
        .prepended("agreementId,purposeId,year,month,day,tokencount")
    )
  }

  def getAgreementRecord(implicit ec: ExecutionContext): Future[List[String]] = {
    logger.info("Gathering Agreements Information")

    ReadModelQueries
      .getAllActiveAgreements(config.collections, readModel)
      .zip(ReadModelQueries.getAllPurposes(config.collections, readModel))
      .map { case (agreements, purposes) =>
        val header = "eservice,producer,consumer,activationDate,purposes,agreementId,eserviceId,consumerId,purposeIds"
        header :: agreements.toList.map { a =>
          val purposeIds: Seq[(String, String)] = purposes
            .filter(p => p.consumerId == a.consumerId && p.eserviceId == a.eserviceId)
            .map(p => (p.purposeId, p.name))
          List(
            a.eservice,
            a.producer,
            a.consumer,
            a.activationDate.getOrElse(""),
            purposeIds.map(_._2).mkString("|"),
            a.agreementId,
            a.eserviceId,
            a.consumerId,
            purposeIds.map(_._1).mkString("|")
          ).map(s => s"\"$s\"").mkString(",")
        }
      }
  }

  def getDescriptorsRecord(implicit ec: ExecutionContext): Future[(List[String], List[String])] = {
    logger.info("Gathering Descriptors Information")

    val header: String                              = "name,createdAt,producerId,descriptorId,state"
    val asCsvRow: Descriptor => String              = (d: Descriptor) =>
      s""""${d.name}","${d.createdAt}","${d.producerId}","${d.descriptorId}","${d.state}""""
    val asCsvRows: List[Descriptor] => List[String] = Functor[List].lift(asCsvRow)
    val addHeader: List[String] => List[String]     = header :: _

    val asCsv: List[Descriptor] => List[String] = asCsvRows.andThen(addHeader)

    ReadModelQueries
      .getAllDescriptors(config.collections, readModel)
      .map(descriptors => (descriptors.toList, descriptors.filter(_.isActive).toList))
      .map(asCsv.split(asCsv))
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
