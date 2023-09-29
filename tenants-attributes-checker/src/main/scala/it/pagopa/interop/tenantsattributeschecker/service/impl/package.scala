package it.pagopa.interop.tenantsattributeschecker.service

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

package object impl extends SprayJsonSupport with DefaultJsonProtocol {
  implicit def mailInfoFormat: RootJsonFormat[MailTemplate] = jsonFormat2(MailTemplate.apply)
}
