package it.pagopa.interop.eservicesmonitoringexporter.model

import io.circe._
import io.circe.generic.semiauto._

sealed trait State

object State {
  case object ACTIVE   extends State
  case object INACTIVE extends State

  implicit val stateEncoder: Encoder.AsObject[State] = deriveEncoder[State]
}
