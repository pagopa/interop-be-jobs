package it.pagopa.interop.dashboardmetricsreportgenerator

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import spray.json.DefaultJsonProtocol._
import spray.json._
import scala.util.Try
import java.time._
import cats.syntax.all._
import it.pagopa.interop.commons.utils.TypeConversions._
import cats.data.Chain

final case class DashboardData(
  descriptors: DescriptorsData,
  tenants: TenantsData,
  agreements: AgreementsData,
  purposes: PurposesData,
  tokens: TokensData
)

final case class GraphElement(time: OffsetDateTime, value: Int)
final case class DescriptorsData(primary: Int, secondary: Int, graph: List[GraphElement])
final case class TenantsData(primary: Int, secondary: Int, graph: List[GraphElement])
final case class AgreementsData(primary: Int, secondary: Int, graph: List[GraphElement])
final case class PurposesData(primary: Int, secondary: Int, graph: List[GraphElement])
final case class TokensData(primary: Int, secondary: Int, graph: List[GraphElement])

object DashboardData {
  private val timeFormat: DateTimeFormatter         = DateTimeFormatter.ISO_DATE_TIME
  implicit val odtf: RootJsonFormat[OffsetDateTime] = new RootJsonFormat[OffsetDateTime] {
    // * This might throw but we'll never read a dashboard data json after all
    override def read(json: JsValue): OffsetDateTime = json match {
      case JsString(s) => OffsetDateTime.parse(s, timeFormat)
      case _           => throw new Exception()
    }
    override def write(obj: OffsetDateTime): JsValue = JsString(obj.format(timeFormat))
  }
  implicit val gef: RootJsonFormat[GraphElement]    = jsonFormat2(GraphElement.apply)
  implicit val ddf: RootJsonFormat[DescriptorsData] = jsonFormat3(DescriptorsData.apply)
  implicit val tdf: RootJsonFormat[TenantsData]     = jsonFormat3(TenantsData.apply)
  implicit val adf: RootJsonFormat[AgreementsData]  = jsonFormat3(AgreementsData.apply)
  implicit val pdf: RootJsonFormat[PurposesData]    = jsonFormat3(PurposesData.apply)
  implicit val todf: RootJsonFormat[TokensData]     = jsonFormat3(TokensData.apply)
  implicit val dadf: RootJsonFormat[DashboardData]  = jsonFormat5(DashboardData.apply)
}

final class TokensReport private (val map: Map[OffsetDateTime, Int]) {
  def lastDate(): LocalDate = map.keySet.max.toLocalDate

  def allButLastDate: TokensReport = {
    val lastDateValue = lastDate()
    new TokensReport(map.filterNot { case (k, _) => k.toLocalDate.isEqual(lastDateValue) })
  }

  def addDate(time: OffsetDateTime): TokensReport = new TokensReport(map.updatedWith(time) {
    case Some(x) => Some(x + 1)
    case None    => Some(1)
  })

  def toList: List[OffsetDateTime] = map
    .foldLeft(Chain.empty[OffsetDateTime]) { case (chain, (time, n)) =>
      chain.concat(Chain.fromSeq(List.fill(n)(time)))
    }
    .toList

  def saveFormat: String =
    map.map { case (k, v) => (k.toInstant().toEpochMilli(), v) }.toJson.compactPrint

}

object TokensReport {
  def from(s: String): Try[TokensReport] =
    Try(s.parseJson.convertTo[Map[String, Int]].toList).flatMap(_.traverse { case (k, v) =>
      Try(k.toLong).flatMap(_.toEuropeRomeOffsetDateTime).map((_, v))
    }.map(l => new TokensReport(l.toMap)))

  def empty: TokensReport = new TokensReport(Map.empty)
}
