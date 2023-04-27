package it.pagopa.interop.eservicesmonitoringexporter.model

import io.circe._
import io.circe.generic.semiauto._

sealed trait Technology

object Technology {
  case object REST extends Technology
  case object SOAP extends Technology

  implicit val technologyEncoder: Encoder.AsObject[Technology] = deriveEncoder[Technology]
}
