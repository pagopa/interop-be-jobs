package it.pagopa.interop.metricsreportgenerator.util

import cats.syntax.all._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import java.time.OffsetDateTime
import it.pagopa.interop.commons.files.service.FileManager
import spray.json._
import it.pagopa.interop.commons.utils.TypeConversions._
import it.pagopa.interop.commons.utils.errors.GenericComponentErrors.GenericError

object Jobs {

  val maxParallelism = 100

  def getTokensData(fileManager: FileManager, config: TokensBucketConfiguration): Future[List[String]] = {

    def getTokenInfo(token: String): Future[(String, String, OffsetDateTime)] =
      Future(token.parseJson.asJsObject.fields)
        .flatMap { fields =>
          for {
            aId   <- fields
              .get("agreementId")
              .collect { case JsString(x) => x }
              .toFuture(GenericError("Missing agreementId field in token"))
            pId   <- fields
              .get("purposeId")
              .collect { case JsString(x) => x }
              .toFuture(GenericError("Missing purposeId field in token"))
            dateL <- fields
              .get("issuedAt")
              .collect { case JsNumber(x) => x.toLong }
              .toFuture(GenericError("Missing issuedAt field in token"))
            date  <- dateL.toOffsetDateTime.toFuture
          } yield (aId, pId, date)
        }

    def allTokensPaths(): Future[List[fileManager.StorageFilePath]] =
      fileManager.listFiles(config.bucket)(config.basePath)

    def getTokens(path: String): Future[List[String]] =
      fileManager.getFile(config.bucket)(path).map(bs => new String(bs).split('\n').toList)

    allTokensPaths()
      .flatMap(paths =>
        Future.traverseWithLatch(maxParallelism)(paths)(path =>
          getTokens(path).flatMap(tokens => Future.traverse(tokens)(getTokenInfo))
        )
      )
      .map(
        _.flatten
          .map { case (aId, pId, time) => (aId, pId, time.getYear, time.getMonthValue, time.getDayOfMonth) }
          .groupMapReduce(identity)(_ => 1)(_ + _) // count occurrences
          .toList
          .sortBy { case ((aId, pId, y, m, d), _) => (aId, pId, y, m, d) }
          .map { case ((aId, pId, y, m, d), c) => s"$aId,$pId,$y,$m,$d,$c" }
          .prepended("agreementId,purposeId,year,month,day,tokencount")
      )
  }
}
