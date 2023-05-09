package it.pagopa.interop.privacynoticesupdater.converters

import it.pagopa.interop.privacynoticesupdater.model.http.{PrivacyNotice, PrivacyNoticeVersion}
import it.pagopa.interop.privacynoticesupdater.model.db.{PrivacyNotice => PrivacyNoticeDb}
import it.pagopa.interop.privacynoticesupdater.model.db.{PrivacyNoticeVersion => PrivacyNoticeVersionDb}

import java.time.ZoneOffset;

object PrivacyNoticeConverter {

  val zoneId = ZoneOffset.UTC

  implicit class PrivacyNoticeWrapper(private val pn: PrivacyNotice) extends AnyVal {
    def toPersistent: PrivacyNoticeDb =
      PrivacyNoticeDb(
        id = pn.id,
        createdDate = pn.createdDate.toInstant(zoneId),
        lastPublishedDate = pn.lastPublishedDate.toInstant(zoneId),
        organizationId = pn.organizationId,
        responsibleUserId = pn.responsibleUserId,
        privacyNoticeVersion = pn.version.toPersistent
      )
  }

  implicit class PrivacyNoticeVersionWrapper(private val pnv: PrivacyNoticeVersion) extends AnyVal {
    def toPersistent: PrivacyNoticeVersionDb =
      PrivacyNoticeVersionDb(
        versionId = pnv.id,
        name = pnv.name,
        publishedDate = pnv.publishedDate.toInstant(zoneId),
        status = pnv.status,
        version = pnv.version
      )
  }
}
