package it.pagopa.interop.tokenreader.messages

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

final case class JWTDetailsMessage(
  jti: String,
  iat: Long,
  exp: Long,
  nbf: Long,
  organizationId: String,
  clientId: String,
  purposeId: Option[String],
  kid: String
)

object JWTDetailsMessage extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val jwtDetailsMessageFormat: RootJsonFormat[JWTDetailsMessage] = jsonFormat8(JWTDetailsMessage.apply)
}
