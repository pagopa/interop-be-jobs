package it.pagopa.interop.certifiedMailSender.model

final case class MailAttachment(name: String, bytes: Array[Byte], mimetype: String)