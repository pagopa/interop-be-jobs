package it.pagopa.interop.metricsreportgenerator.report

import java.time.OffsetDateTime

final case class MetricGeneratorSeed(
  version: String,
  state: String,
  createdAt: OffsetDateTime,
  activatedAt: OffsetDateTime,
  fileExtractedMetrics: FileExtractedMetrics
)
