package it.pagopa.interop.privacynoticesupdater.converters

import it.pagopa.interop.privacynoticesupdater.model.http.{PrivacyNotice, PrivacyNoticeVersion}
import it.pagopa.interop.privacynoticesupdater.model.db.{PrivacyNotice => PrivacyNoticeDb}
import it.pagopa.interop.privacynoticesupdater.model.db.{PrivacyNoticeVersion => PrivacyNoticeVersionDb}

import java.time.{ZoneOffset, ZoneId};
import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier
import java.time.temporal.ChronoUnit

object PrivacyNoticeConverter {

  val zoneUtc = ZoneOffset.UTC
  val zoneCet = ZoneId.of("Europe/Rome")

  implicit class PrivacyNoticeWrapper(private val pn: PrivacyNotice) extends AnyVal {
    def toPersistent: PrivacyNoticeDb =
      PrivacyNoticeDb(
        privacyNoticeId = pn.id,
        createdDate = pn.createdDate.atZone(zoneCet).toInstant().atOffset(zoneUtc),
        lastPublishedDate = pn.lastPublishedDate.atZone(zoneCet).toInstant().atOffset(zoneUtc),
        organizationId = pn.organizationId,
        responsibleUserId = pn.responsibleUserId,
        privacyNoticeVersion = pn.version.toPersistent,
        persistedAt = OffsetDateTimeSupplier.get().truncatedTo(ChronoUnit.SECONDS)
      )
  }

  implicit class PrivacyNoticeVersionWrapper(private val pnv: PrivacyNoticeVersion) extends AnyVal {
    def toPersistent: PrivacyNoticeVersionDb =
      PrivacyNoticeVersionDb(
        versionId = pnv.id,
        name = pnv.name,
        publishedDate = pnv.publishedDate.atZone(zoneCet).toInstant().atOffset(zoneUtc),
        status = pnv.status,
        version = pnv.version
      )
  }
}
