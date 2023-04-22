package it.pagopa.interop.eservicesfilegenerator.model

import io.circe._
import io.circe.generic.semiauto._
import spray.json.DefaultJsonProtocol._
import it.pagopa.interop.commons.utils.SprayCommonFormats.uuidFormat
import spray.json._
import java.util.UUID

final case class EService(
  name: String,
  eserviceId: UUID,
  versionId: UUID,
  technology: Technology,
  state: State,
  basePath: Seq[String],
  producerName: String,
  version: String
)

object EService {
  implicit val stateEncoder: Encoder.AsObject[State]           = deriveEncoder[State]
  implicit val technologyEncoder: Encoder.AsObject[Technology] = deriveEncoder[Technology]
  implicit val eServiceEncoder: Encoder.AsObject[EService]     = deriveEncoder[EService]

  implicit val eServiceFormat: RootJsonFormat[EService] = jsonFormat8(EService.apply)
}
