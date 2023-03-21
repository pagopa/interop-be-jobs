package it.pagopa.interop.certifiedMailSender.model

import java.util.UUID

final case class MailAttachment(name: String, bytes: Array[Byte], mimetype: String)
final case class InteropEnvelope(
  id: UUID,
  recipients: List[String],
  subject: String,
  body: String,
  attachments: List[MailAttachment]
)
