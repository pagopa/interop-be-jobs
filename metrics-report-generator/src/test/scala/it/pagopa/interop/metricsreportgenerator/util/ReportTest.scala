package it.pagopa.interop.metricsreportgenerator.util

import munit.FunSuite
import scala.io.Source
import it.pagopa.interop.metricsreportgenerator.util.models.Report
import it.pagopa.interop.commons.utils.TypeConversions._
import scala.util._
import java.time.LocalDate

class ReportTest extends FunSuite {

  test("The last date should be correct") {
    Report.from(Source.fromResource("tokens.csv").getLines().mkString("\n").getBytes) match {
      case Failure(e)      => fail("Cannot read the file", e)
      case Success(report) =>
        assertEquals(report.lastDate, LocalDate.of(2023, 8, 10))
        assertEquals(report.allButLastDate.lastDate, LocalDate.of(2023, 8, 9))
    }
  }

  test("Date should be correctly parsed and extracted") {
    assertEquals(1691668694000L.toOffsetDateTime.get.toLocalDate(), LocalDate.of(2023, 8, 10))
  }
}
