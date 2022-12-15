package it.pagopa.interop.metricsreportgenerator.models

import io.circe._
import io.circe.generic.semiauto._
import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier

import java.time.OffsetDateTime
final case class Metric(
  originId: String,
  origin: String,
  name: String,
  technology: String,
  openData: Boolean,
  version: String,
  fingerPrint: String,
  endpointsCount: Int,
  activatedAt: OffsetDateTime,
  createdAt: OffsetDateTime
)

object Metric {

  implicit val metricEncoder: Encoder.AsObject[Metric] = deriveEncoder[Metric]

  def generator(originId: String, origin: String, name: String, technology: String, openData: Boolean = false)(
    dateTimeSupplier: OffsetDateTimeSupplier
  ): (String, OffsetDateTime, FileMetricInfo) => Metric = (version, activatedAt, fileMetricInfo) =>
    Metric(
      originId = originId,
      origin = origin,
      name = name,
      technology = technology,
      openData = openData,
      version = version,
      fingerPrint = fileMetricInfo.fingerPrint,
      endpointsCount = fileMetricInfo.endpointsCount,
      activatedAt = activatedAt,
      createdAt = dateTimeSupplier.get()
    )

}
