package it.pagopa.interop.tenantsattributeschecker.util

import it.pagopa.interop.commons.utils.SprayCommonFormats.{offsetDateTimeFormat, uuidFormat}
import it.pagopa.interop.tenantmanagement.model.persistence.JsonFormats.pvaFormat
import it.pagopa.interop.tenantmanagement.model.tenant.{
  PersistentTenantMail,
  PersistentTenantMailKind,
  PersistentVerifiedAttribute
}
import spray.json.DefaultJsonProtocol._
import spray.json.{DeserializationException, JsString, JsValue, RootJsonFormat}

import java.util.UUID

final case class Tenant(id: UUID, attributes: PersistentVerifiedAttribute, mails: List[PersistentTenantMail])

object Tenant {

  private implicit val ptmkFormat: RootJsonFormat[PersistentTenantMailKind] =
    new RootJsonFormat[PersistentTenantMailKind] {
      override def read(json: JsValue): PersistentTenantMailKind = json match {
        case JsString("CONTACT_EMAIL") => PersistentTenantMailKind.ContactEmail
        case x => throw DeserializationException(s"Unable to deserialize PersistentTenantKind: unmapped kind $x")
      }

      override def write(obj: PersistentTenantMailKind): JsValue = obj match {
        case PersistentTenantMailKind.ContactEmail => JsString("CONTACT_EMAIL")
      }
    }

  implicit val ptmFormat: RootJsonFormat[PersistentTenantMail] = jsonFormat4(PersistentTenantMail.apply)
  implicit val format: RootJsonFormat[Tenant]                  = jsonFormat3(Tenant.apply)
}