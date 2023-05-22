package it.pagopa.interop.eservicesmonitoringexporter.model

import io.circe._
import io.circe.generic.semiauto._
import java.util.UUID

final case class EService(
  name: String,
  eserviceId: UUID,
  versionId: UUID,
  technology: Technology,
  state: State,
  basePath: Seq[String],
  producerName: String,
  versionNumber: Int,
  audience: Seq[String]
)

object EService {
  implicit val eServiceEncoder: Encoder.AsObject[EService] = deriveEncoder[EService]
}
