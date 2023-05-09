package it.pagopa.interop.privacynoticesupdater.model.http

import java.util.UUID
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import io.circe.generic.semiauto._
import io.circe.{Encoder, Decoder}

final case class Authorization(access_token: String, scope: String, token_type: String, refresh_token: Option[String])

final case class PrivacyNotice(
  id: UUID,
  createdDate: LocalDateTime,
  lastPublishedDate: LocalDateTime,
  organizationId: UUID,
  responsibleUserId: Option[UUID],
  version: PrivacyNoticeVersion
)

final case class PrivacyNoticeVersion(
  id: UUID,
  name: String,
  publishedDate: LocalDateTime,
  status: String,
  version: Int
)

object ModelFormats {
  val dateFormatter            = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
  implicit val dateTimeEncoder = Encoder.encodeLocalDateTimeWithFormatter(dateFormatter)
  implicit val dateTimeDecoder = Decoder.decodeLocalDateTimeWithFormatter(dateFormatter)

  implicit val authorizationFormat: Decoder[Authorization]               = deriveDecoder
  implicit val privacyNoticeVersionFormat: Decoder[PrivacyNoticeVersion] = deriveDecoder
  implicit val privacyNoticeFormat: Decoder[PrivacyNotice]               = deriveDecoder

}
