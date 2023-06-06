package it.pagopa.interop.eservicesmonitoringexporter.model

import io.circe._
import io.circe.generic.semiauto._
import java.util.UUID

final case class EService(
  name: String,
  eserviceId: UUID,
  versionId: UUID,
  technology: String,
  state: String,
  basePath: Seq[String],
  producerName: String,
  versionNumber: Int,
  audience: Seq[String]
)

sealed trait State

object State {
  case object ACTIVE   extends State
  case object INACTIVE extends State
}

sealed trait Technology

object Technology {
  case object REST extends Technology
  case object SOAP extends Technology
}

object EService {
  implicit val eServiceEncoder: Encoder.AsObject[EService] = deriveEncoder[EService]
}
