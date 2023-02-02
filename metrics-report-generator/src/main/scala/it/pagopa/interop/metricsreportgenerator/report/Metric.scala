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
  state: String,
  fingerPrint: String,
  endpointsCount: Int,
  createdAt: Long,
  activatedAt: Long,
  timestamp: Long
)

object Metric {

  implicit val metricEncoder: Encoder.AsObject[Metric] = deriveEncoder[Metric]

  def generator(
    originId: String,
    origin: String,
    name: String,
    eServiceId: String,
    technology: String,
    timestamp: OffsetDateTime,
    openData: Boolean = false
  ): MetricGeneratorSeed => Metric =
    seed =>
      Metric(
        originId = originId,
        origin = origin,
        name = name,
        eServiceId = eServiceId,
        technology = technology,
        openData = openData,
        version = seed.version,
        state = seed.state,
        fingerPrint = seed.fileExtractedMetrics.fingerPrint,
        endpointsCount = seed.fileExtractedMetrics.endpointsCount,
        createdAt = seed.createdAt.toMillis,
        activatedAt = seed.activatedAt.toMillis,
        timestamp = timestamp.toMillis
      )

}
