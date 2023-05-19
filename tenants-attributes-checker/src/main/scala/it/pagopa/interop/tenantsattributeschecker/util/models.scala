//package it.pagopa.interop.tenantsattributeschecker.util
//
//import cats.implicits.catsSyntaxOptionId
//import it.pagopa.interop.commons.utils.SprayCommonFormats.{offsetDateTimeFormat, uuidFormat}
//import it.pagopa.interop.tenantmanagement.model.tenant.PersistentVerificationRenewal.{
//  AUTOMATIC_RENEWAL,
//  REVOKE_ON_EXPIRATION
//}
//import it.pagopa.interop.tenantmanagement.client.model.{
//  CertifiedTenantAttribute,
//  DeclaredTenantAttribute,
//  TenantAttribute,
//  TenantRevoker,
//  TenantVerifier,
//  VerificationRenewal,
//  VerifiedTenantAttribute
//}
//import it.pagopa.interop.tenantmanagement.model.persistence.JsonFormats.pvaFormat
//import it.pagopa.interop.tenantmanagement.model.tenant._
//import spray.json.DefaultJsonProtocol._
//import spray.json.{DeserializationException, JsString, JsValue, RootJsonFormat}
//
//import java.util.UUID
//
//final case class Tenant(id: UUID, attributes: PersistentVerifiedAttribute, mails: List[PersistentTenantMail])
//final case class TenantData(id: UUID, attributesExpired: Seq[PersistentVerifiedAttribute])
//
//object Tenant {
//  // TODO è possibile recuperarli da it.pagopa.interop.tenantmanagement.model.persistence.JsonFormats? Dovremmo levargli il private?
//
//  private implicit val ptmkFormat: RootJsonFormat[PersistentTenantMailKind] =
//    new RootJsonFormat[PersistentTenantMailKind] {
//      override def read(json: JsValue): PersistentTenantMailKind = json match {
//        case JsString("CONTACT_EMAIL") => PersistentTenantMailKind.ContactEmail
//        case x => throw DeserializationException(s"Unable to deserialize PersistentTenantKind: unmapped kind $x")
//      }
//
//      override def write(obj: PersistentTenantMailKind): JsValue = obj match {
//        case PersistentTenantMailKind.ContactEmail => JsString("CONTACT_EMAIL")
//      }
//    }
//
//  implicit val ptmFormat: RootJsonFormat[PersistentTenantMail] = jsonFormat4(PersistentTenantMail.apply)
//  implicit val format: RootJsonFormat[Tenant]                  = jsonFormat3(Tenant.apply)
//}
//
//object Adapters {
//  // TODO è possibile recuperarli da it.pagopa.interop.tenantmanagement.model.persistence.Adapters?
//
//  implicit class PersistentVerificationRenewalWrapper(private val p: PersistentVerificationRenewal) extends AnyVal {
//    def toAPI(): VerificationRenewal = p match {
//      case AUTOMATIC_RENEWAL    => VerificationRenewal.AUTOMATIC_RENEWAL
//      case REVOKE_ON_EXPIRATION => VerificationRenewal.REVOKE_ON_EXPIRATION
//    }
//  }
//
//  implicit class PersistentVerificationTenantVerifierWrapper(private val p: PersistentTenantVerifier) extends AnyVal {
//    def toAPI(): TenantVerifier = TenantVerifier(
//      id = p.id,
//      verificationDate = p.verificationDate,
//      renewal = p.renewal.toAPI(),
//      expirationDate = p.expirationDate,
//      extensionDate = p.expirationDate
//    )
//  }
//
//  implicit class PersistentVerificationTenantRevokerWrapper(private val p: PersistentTenantRevoker) extends AnyVal {
//    def toAPI(): TenantRevoker = TenantRevoker(
//      id = p.id,
//      verificationDate = p.verificationDate,
//      renewal = p.renewal.toAPI(),
//      expirationDate = p.expirationDate,
//      extensionDate = p.expirationDate,
//      revocationDate = p.revocationDate
//    )
//  }
//
//  implicit class PersistentAttributesWrapper(private val p: PersistentTenantAttribute) extends AnyVal {
//    def toAPI: TenantAttribute = p match {
//      case a: PersistentCertifiedAttribute =>
//        TenantAttribute(certified = CertifiedTenantAttribute(a.id, a.assignmentTimestamp, a.revocationTimestamp).some)
//      case a: PersistentDeclaredAttribute  =>
//        TenantAttribute(declared = DeclaredTenantAttribute(a.id, a.assignmentTimestamp, a.revocationTimestamp).some)
//      case a: PersistentVerifiedAttribute  =>
//        TenantAttribute(verified =
//          VerifiedTenantAttribute(
//            a.id,
//            a.assignmentTimestamp,
//            a.verifiedBy.map(_.toAPI()),
//            a.revokedBy.map(_.toAPI())
//          ).some
//        )
//    }
//  }
//
//}
