package it.pagopa.interop.metricsreportgenerator.util.models

import java.time.LocalDate
import it.pagopa.interop.commons.utils.errors.GenericComponentErrors.GenericError
import it.pagopa.interop.commons.utils.TypeConversions._
import scala.util._
import spray.json.DefaultJsonProtocol._
import spray.json._
import cats.syntax.all._

final case class Agreement(
  activationDate: Option[String],
  agreementId: String,
  eserviceId: String,
  eservice: String,
  producer: String,
  consumer: String,
  consumerId: String
)

object Agreement {
  implicit val format: RootJsonFormat[Agreement] = jsonFormat7(Agreement.apply)
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
  state: String
) {
  def isActive: Boolean = state == "Suspended" || state == "Published" || state == "Deprecated"
}

object Descriptor {
  implicit val format: RootJsonFormat[Descriptor] = jsonFormat6(Descriptor.apply)
}

final case class Report private (map: Map[Report.RecordValue, Int]) {
  def add(record: String): Try[Report] = Report
    .extractDataFromToken(record)
    .map(key =>
      Report(map.updatedWith(key) {
        case Some(x) => Option(x + 1)
        case None    => Option(1)
      })
    )

  def addMany(records: List[String]): Try[Report] = {
    def loop(report: Report)(rest: List[String]): Try[Report] = rest match {
      case head :: next => report.add(head).flatMap(loop(_)(next))
      case Nil          => Success(report)
    }

    loop(this)(records)
  }

  def lastDate: LocalDate = map.keySet.map { case (_, _, date) => date }.max

  def allButLastDate: Report = Report(map.filterNot { case ((_, _, date), _) => date.isEqual(lastDate) })

  def render: String = (Report.header :: map.map(Report.renderLine).toList).mkString("\n")
}

object Report {
  type RecordValue = (String, String, LocalDate)

  private def extractDataFromToken(token: String): Try[RecordValue] = Try {
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
    val time: LocalDate              = dateL.toOffsetDateTime.get.toLocalDate()
    (aId, pId, time)
  }

  private val header: String = "agreementId,purposeId,year,month,day,tokencount"

  private val row = raw""""([\w|-]{36})","([\w|-]{36})","(\d{4})","(\d*)","(\d*)","(\d*)"""".r

  def parseCSVLine(line: String): Try[(RecordValue, Int)] = line match {
    case row(aId, pId, year, month, day, count) =>
      Try(LocalDate.of(year.toInt, month.toInt, day.toInt)).flatMap(date => Try(((aId, pId, date), count.toInt)))
    case _                                      => Failure(GenericError(s"Csv line hasn't the right format: $line"))
  }

  private def renderLine(row: (RecordValue, Int)): String = row match {
    case ((aId, pId, time), count) =>
      val year: String  = f"${time.getYear()}%04d"
      val month: String = f"${time.getMonthValue()}%02d"
      val day: String   = f"${time.getDayOfMonth()}%02d"
      s""""${aId}","${pId}","${year}","${month}","${day}","${count}""""
  }

  def from(bytes: Array[Byte]): Try[Report] =
    new String(bytes).split("\n").toList.tail.traverse(parseCSVLine).map(_.toMap).map(new Report(_))

  val empty: Report = new Report(Map.empty[RecordValue, Int])
}
