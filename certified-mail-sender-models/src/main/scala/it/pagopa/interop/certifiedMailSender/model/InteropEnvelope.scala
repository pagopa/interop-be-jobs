package it.pagopa.interop.certifiedMailSender.model

import java.util.UUID

final case class InteropEnvelope(
  id: UUID,
  to: List[String],
  cc: List[String],
  bcc: List[String],
  subject: String,
  body: String,
  attachments: List[MailAttachment]
)