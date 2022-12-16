package it.pagopa.interop.metricsreportgenerator.models

import io.circe._
import io.circe.generic.semiauto._
import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier
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
    openData: Boolean = false
  )(dateTimeSupplier: OffsetDateTimeSupplier): (String, OffsetDateTime, FileExtractedMetrics) => Metric =
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
        createdAt = dateTimeSupplier.get().toMillis
      )

}
