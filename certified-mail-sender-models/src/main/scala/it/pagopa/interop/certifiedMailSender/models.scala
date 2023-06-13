package it.pagopa.interop.certifiedMailSender

import java.util.UUID

final case class MailAttachment(name: String, bytes: Array[Byte], mimetype: String) {
  override def equals(obj: Any): Boolean = obj match {
    case MailAttachment(a, b, c) => name == a && c == mimetype && bytes.sameElements(b)
    case _ => false
  }
}
final case class InteropEnvelope(
  id: UUID,
  recipients: List[String],
  subject: String,
  body: String,
  attachments: List[MailAttachment]
)
