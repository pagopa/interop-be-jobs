package it.pagopa.interop.privacynoticesupdater.model.db

import org.scanamo.DynamoFormat
import org.scanamo.generic.semiauto.deriveDynamoFormat

import java.util.UUID
import java.time.Instant

final case class PrivacyNotice(
  id: UUID,
  createdDate: Instant,
  lastPublishedDate: Instant,
  organizationId: UUID,
  responsibleUserId: Option[UUID],
  privacyNoticeVersion: PrivacyNoticeVersion
)

final case class PrivacyNoticeVersion(
  versionId: UUID,
  name: String,
  publishedDate: Instant,
  status: String,
  version: Int
)

object PrivacyNotice {
  implicit val formatPrivacyNoticeVersion: DynamoFormat[PrivacyNoticeVersion] = deriveDynamoFormat
  implicit val formatPrivacyNotice: DynamoFormat[PrivacyNotice]               = deriveDynamoFormat
}
