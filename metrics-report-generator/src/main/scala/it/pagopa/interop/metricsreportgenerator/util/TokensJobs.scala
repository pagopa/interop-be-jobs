package it.pagopa.interop.metricsreportgenerator.util

import cats.syntax.all._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import com.typesafe.scalalogging.LoggerTakingImplicit
import it.pagopa.interop.commons.logging._
import scala.util.Try
import it.pagopa.interop.commons.utils.TypeConversions._
import java.time.LocalDate
import scala.jdk.CollectionConverters._
import java.util.stream.Collectors
import it.pagopa.interop.metricsreportgenerator.util.models.Report

class TokensJobs(s3: S3)(implicit
  logger: LoggerTakingImplicit[ContextFieldsToLog],
  context: ContextFieldsToLog,
  ec: ExecutionContext
) {

  private def datesUntilToday(startDate: LocalDate): List[LocalDate] = {
    val today = LocalDate.now()
    if (startDate.equals(today)) List(today)
    else
      startDate
        .datesUntil(today)
        .collect(Collectors.toList[LocalDate]())
        .asScala
        .toList
        .appended(today)
  }

  private def updateReport(report: Report)(tokenFilesPath: List[String]): Future[Report] = {
    val getTokenLines: String => Future[List[String]] =
      path => s3.getToken(path).map(bs => new String(bs).split('\n').toList)

    Future
      .sequentiallyAccumulateLeft(tokenFilesPath)(getTokenLines)(Try(report)) { case (tryReport, records) =>
        tryReport.flatMap(_.addMany(records))
      }
      .flatMap(_.toFuture)
  }

  def getTokensData: Future[String] = {
    logger.info("Gathering tokens information")

    s3.readTokensReport()
      .flatMap {
        case None         =>
          logger.info("Tokens report not found, calculating from scratch")
          s3.listAllTokens().flatMap(updateReport(Report.empty))
        case Some(report) =>
          logger.info("Tokens report found, calculating delta")
          val missingDays: List[LocalDate] = datesUntilToday(report.lastDate)
          val prunedReport: Report         = report.allButLastDate

          // ! Se rirunnato aggiorna ieri, non oggi, perche'?

          asdsad

          Future
            .traverseWithLatch(10)(missingDays)(s3.listTokensForDay)
            .map(_.flatten)
            .flatMap(updateReport(prunedReport))
      }
      .map(_.render)
  }

}
