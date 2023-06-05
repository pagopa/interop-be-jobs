package it.pagopa.interop.tenantsattributeschecker.service

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import it.pagopa.interop.certifiedMailSender.{InteropEnvelope, MailAttachment}
import it.pagopa.interop.commons.utils.SprayCommonFormats.uuidFormat
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

package object impl extends SprayJsonSupport with DefaultJsonProtocol {
  implicit def mailAttachmentFormat: RootJsonFormat[MailAttachment]  = jsonFormat3(MailAttachment)
  implicit def interopEnvelopFormat: RootJsonFormat[InteropEnvelope] = jsonFormat5(InteropEnvelope)
  implicit def mailInfoFormat: RootJsonFormat[MailTemplate]          = jsonFormat2(MailTemplate.apply)
}
