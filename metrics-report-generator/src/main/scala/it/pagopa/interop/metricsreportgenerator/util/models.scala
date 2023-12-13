package it.pagopa.interop.metricsreportgenerator.util.models

import cats.syntax.all._
import it.pagopa.interop.commons.utils.errors.GenericComponentErrors.GenericError
import it.pagopa.interop.metricsreportgenerator.util.Errors.ChecksumNotFound
import spray.json.DefaultJsonProtocol._
import spray.json._

import java.time.{Instant, LocalDate, ZoneId}
import scala.concurrent.Future
import scala.util._
import spoiwo.model._
import spoiwo.model.enums._
import scala.collection.immutable.ListMap

final case class Agreement(
  activationDate: Option[String],
  agreementId: String,
  eserviceId: String,
  eservice: String,
  producer: String,
  producerId: String,
  consumer: String,
  consumerId: String
)

object Agreement {
  implicit val format: RootJsonFormat[Agreement] = jsonFormat8(Agreement.apply)
}

final case class Purpose(purposeId: String, consumerId: String, eserviceId: String, name: String)

object Purpose {
  implicit val format: RootJsonFormat[Purpose] = jsonFormat4(Purpose.apply)
}

final case class Descriptor(
  name: String,
  createdAt: String,
  producerId: String,
  producer: String,
  descriptorId: String,
  state: String,
  checksum: Option[String]
) {
  def isActive: Boolean = state == "Suspended" || state == "Published" || state == "Deprecated"

  def toMetric: Future[MetricDescriptor] = checksum
    .fold[Future[MetricDescriptor]](Future.failed(ChecksumNotFound(descriptorId)))(chs =>
      Future.successful(MetricDescriptor(name, createdAt, producerId, producer, descriptorId, state, chs))
    )

}

object Descriptor {
  implicit val format: RootJsonFormat[Descriptor] = jsonFormat7(Descriptor.apply)
}

final case class MetricDescriptor(
  name: String,
  createdAt: String,
  producerId: String,
  producer: String,
  descriptorId: String,
  state: String,
  fingerprint: String
)

object SheetStyle {

  val headerStyle =
    CellStyle(
      fillPattern = CellFill.Solid,
      fillForegroundColor = Color.LightGreen,
      font = Font(bold = true),
      locked = true,
      borders = CellBorders(
        leftStyle = CellBorderStyle.Thin,
        bottomStyle = CellBorderStyle.Thin,
        rightStyle = CellBorderStyle.Thin,
        topStyle = CellBorderStyle.Thin
      ),
      horizontalAlignment = CellHorizontalAlignment.Center,
      verticalAlignment = CellVerticalAlignment.Center,
      wrapText = true
    )

  val rowStyle =
    CellStyle(
      fillPattern = CellFill.Solid,
      fillForegroundColor = Color.White,
      font = Font(bold = false),
      borders = CellBorders(
        leftStyle = CellBorderStyle.Thin,
        bottomStyle = CellBorderStyle.Thin,
        rightStyle = CellBorderStyle.Thin,
        topStyle = CellBorderStyle.Thin
      )
    )
}

final case class Report private (map: Map[Report.RecordValue, Int]) {
  def addIfInRange(after: Instant, before: Instant)(record: String): Try[Report] = Report
    .extractDataFromToken(record)
    .map { case (aid, pid, instant) =>
      if (instant.isAfter(after) && instant.isBefore(before)) {
        Report(map.updatedWith((aid, pid, instant.atZone(Report.europeRome).toLocalDate())) {
          case Some(x) => Option(x + 1)
          case None    => Option(1)
        })
      } else this
    }

  def addAllTokensIssuedInRange(after: Instant, before: Instant)(records: List[String]): Try[Report] =
    records.foldLeft(Try(this)) { case (report, record) =>
      report.flatMap(_.addIfInRange(after, before)(record))
    }

  def lastDate: LocalDate = map.keySet.map { case (_, _, date) => date }.max

  def allButLastDate: Report = Report(map.filterNot { case ((_, _, date), _) => date.isEqual(lastDate) })

  def renderCsv: String = (Report.headerCsv :: map.map(Report.renderLineCsv).toList.sorted).mkString("\n")

  def renderSheet: Sheet = {
    val headerRow =
      Row(style = SheetStyle.headerStyle).withCellValues(Report.headerSheet)
    val columns   = (0 until (Report.headerSheet.size)).toList
      .map(index => Column(index = index, style = CellStyle(font = Font(bold = true)), autoSized = true))

    val rows = ListMap(map.toSeq.sorted: _*).map(Report.renderLineSheet).toList
    Sheet(name = "Tokens", rows = headerRow :: rows, columns = columns)
  }
}

object Report {
  type RecordValue    = Tuple3[String, String, LocalDate]
  type RawRecordValue = (String, String, Instant)

  val europeRome: ZoneId = ZoneId.of("Europe/Rome")

  private def extractDataFromToken(token: String): Try[RawRecordValue] = Try {
    val fields: Map[String, JsValue] = token.parseJson.asJsObject.fields
    val aId: String                  = fields
      .get("agreementId")
      .collect { case JsString(x) => x }
      .getOrElse(throw GenericError("Missing or broken agreementId field in token"))
    val pId: String                  = fields
      .get("purposeId")
      .collect { case JsString(x) => x }
      .getOrElse(throw GenericError("Missing or broken purposeId field in token"))
    val dateL: Long                  = fields
      .get("issuedAt")
      .collect { case JsNumber(x) => x.toLong }
      .getOrElse(throw GenericError("Missing or broken issuedAt field in token"))
    val time: Instant                = Instant.ofEpochMilli(dateL)
    (aId, pId, time)
  }

  private val headerCsv: String         = "agreementId,purposeId,year,month,day,tokencount"
  private val headerSheet: List[String] = List("agreementId", "purposeId", "year", "month", "day", "tokencount")

  private val row = raw""""([\w|-]{36})","([\w|-]{36})","(\d{4})","(\d*)","(\d*)","(\d*)"""".r

  def parseCSVLine(line: String): Try[(RecordValue, Int)] = line match {
    case row(aId, pId, year, month, day, count) =>
      Try(LocalDate.of(year.toInt, month.toInt, day.toInt)).flatMap(date => Try(((aId, pId, date), count.toInt)))
    case _                                      => Failure(GenericError(s"Csv line hasn't the right format: $line"))
  }

  private def renderLineCsv(row: (RecordValue, Int)): String = row match {
    case ((aId, pId, time), count) =>
      val year: String  = f"${time.getYear()}%04d"
      val month: String = f"${time.getMonthValue()}%02d"
      val day: String   = f"${time.getDayOfMonth()}%02d"
      s""""${aId}","${pId}","${year}","${month}","${day}","${count}""""
  }

  private def renderLineSheet(row: (RecordValue, Int)): Row = row match {
    case ((aId, pId, time), count) =>
      val year: String  = f"${time.getYear()}%04d"
      val month: String = f"${time.getMonthValue()}%02d"
      val day: String   = f"${time.getDayOfMonth()}%02d"

      Row(style = SheetStyle.rowStyle).withCellValues(aId, pId, year, month, day, count)
  }

  def from(bytes: Array[Byte]): Try[Report] =
    new String(bytes).split("\n").toList.tail.traverse(parseCSVLine).map(_.toMap).map(new Report(_))

  val empty: Report = new Report(Map.empty[RecordValue, Int])
}
