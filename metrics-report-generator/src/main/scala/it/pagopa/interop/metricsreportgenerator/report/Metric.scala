package it.pagopa.interop.metricsreportgenerator.report

import io.circe._
import io.circe.generic.semiauto._
import it.pagopa.interop.commons.utils.TypeConversions.OffsetDateTimeOps

import java.time.OffsetDateTime
final case class Metric(
  originId: String,
  origin: String,
  name: String,
  eServiceId: String,
  technology: String,
  openData: Boolean,
  version: String,
  fingerPrint: String,
  endpointsCount: Int,
  activatedAt: Long,
  createdAt: Long
)

object Metric {

  implicit val metricEncoder: Encoder.AsObject[Metric] = deriveEncoder[Metric]

  def generator(
    originId: String,
    origin: String,
    name: String,
    eServiceId: String,
    technology: String,
    createdAt: OffsetDateTime,
    openData: Boolean = false
  ): (String, OffsetDateTime, FileExtractedMetrics) => Metric =
    (version, activatedAt, fileMetricInfo) =>
      Metric(
        originId = originId,
        origin = origin,
        name = name,
        eServiceId = eServiceId,
        technology = technology,
        openData = openData,
        version = version,
        fingerPrint = fileMetricInfo.fingerPrint,
        endpointsCount = fileMetricInfo.endpointsCount,
        activatedAt = activatedAt.toMillis,
        createdAt = createdAt.toMillis
      )

}
