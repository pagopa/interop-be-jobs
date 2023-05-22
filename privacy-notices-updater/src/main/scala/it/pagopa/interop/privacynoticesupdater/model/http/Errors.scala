package it.pagopa.interop.privacynoticesupdater.model.http

import io.circe.generic.semiauto._
import io.circe.Decoder

case class ErrorMessage(message: Option[String], error: Option[String], path: Option[String])

final case class OneTrustAuthenticationError(error: String, error_description: String)

object ErrorFormats {

  implicit val payloadJsonFormat1: Decoder[ErrorMessage]                = deriveDecoder
  implicit val payloadJsonFormat4: Decoder[OneTrustAuthenticationError] = deriveDecoder
}
