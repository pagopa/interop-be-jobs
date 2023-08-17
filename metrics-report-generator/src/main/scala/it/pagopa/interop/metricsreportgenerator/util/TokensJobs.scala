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
import java.time.Instant
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

class TokensJobs(s3: S3)(implicit
  logger: LoggerTakingImplicit[ContextFieldsToLog],
  context: ContextFieldsToLog,
  ec: ExecutionContext
) {

  private def datesUntilToday(startDate: LocalDate): List[LocalDate] = startDate
    .datesUntil(LocalDate.now())
    .collect(Collectors.toList[LocalDate]())
    .asScala
    .toList
    .appended(LocalDate.now())

  private def updateReport(afterThan: Instant, beforeThan: Instant)(
    report: Report
  )(tokenFilesPath: List[String]): Future[Report] = {
    val getTokenLines: String => Future[List[String]] =
      path => s3.getToken(path).map(bs => new String(bs).split('\n').toList)

    Future // TODO configure parallelism from conf
      .accumulateLeft(10)(tokenFilesPath)(getTokenLines)(Try(report)) { case (tryReport, records) =>
        tryReport.flatMap(_.addAllTokensIssuedInRange(afterThan, beforeThan)(records))
      }
      .flatMap(_.toFuture)
  }

  def getTokensData: Future[String] = {
    logger.info("Gathering tokens information")

    // * The data on S3 is skewed, meaning that a specific date-folder (i.e.
    // * 2023-02-01) may contain tokens issued on the previous day due to
    // * timezone+DST misconfiguration in the token producer. For this reason,
    // * this best effort solution was necessary: we list the tokens until TODAY
    // * INCLUDED, but we list them one by one using today's midnight.
    // * In this way this job should theoretically be idempotent.

    val beforeThan: Instant =
      Instant.from(OffsetDateTime.now(Report.europeRome).truncatedTo(ChronoUnit.DAYS))

    s3.readTokensReport()
      .flatMap {
        case None         =>
          logger.info("Tokens report not found, calculating from scratch")
          s3.listAllTokens().flatMap(updateReport(Instant.MIN, beforeThan)(Report.empty))
        case Some(report) =>
          logger.info("Tokens report found, calculating delta")
          val lastDate                     = report.lastDate
          val afterThan                    = Instant.from(lastDate.atStartOfDay(Report.europeRome))
          val missingDays: List[LocalDate] = datesUntilToday(lastDate)
          val prunedReport: Report         = report.allButLastDate

          Future
            .traverseWithLatch(10)(missingDays)(s3.listTokensForDay)
            .map(_.flatten)
            .flatMap(updateReport(afterThan, beforeThan)(prunedReport))
      }
      .map(_.render)
  }

}
