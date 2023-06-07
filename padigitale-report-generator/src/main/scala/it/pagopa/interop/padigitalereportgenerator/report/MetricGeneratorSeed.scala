package it.pagopa.interop.padigitalereportgenerator.report

import java.time.OffsetDateTime

final case class MetricGeneratorSeed(
  version: String,
  state: String,
  createdAt: OffsetDateTime,
  publishedAt: OffsetDateTime,
  fileExtractedMetrics: FileExtractedMetrics
)
