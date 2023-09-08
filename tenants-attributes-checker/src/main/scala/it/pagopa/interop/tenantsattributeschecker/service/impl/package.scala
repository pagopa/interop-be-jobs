package it.pagopa.interop.tenantsattributeschecker.service

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import it.pagopa.interop.commons.mail.{InteropEnvelope, MailAttachment}
import javax.mail.internet.InternetAddress
import it.pagopa.interop.commons.utils.SprayCommonFormats.uuidFormat
import spray.json.{DefaultJsonProtocol, RootJsonFormat}
import spray.json._

package object impl extends SprayJsonSupport with DefaultJsonProtocol {
  implicit def mailAttachmentFormat: RootJsonFormat[MailAttachment] = jsonFormat3(MailAttachment)

  implicit object InternetAddressFormat extends JsonFormat[InternetAddress] {
    def write(ia: InternetAddress) = JsString(s"${ia.getAddress()}")
    def read(json: JsValue)        = json match {
      case JsString(address) => new InternetAddress(address)
      case _                 => deserializationError("String expected")
    }
  }
  implicit def interopEnvelopFormat: RootJsonFormat[InteropEnvelope] = jsonFormat5(InteropEnvelope)
  implicit def mailInfoFormat: RootJsonFormat[MailTemplate] = jsonFormat2(MailTemplate.apply)
}
