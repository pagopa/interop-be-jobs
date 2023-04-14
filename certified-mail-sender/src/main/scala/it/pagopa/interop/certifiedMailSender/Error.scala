package it.pagopa.interop.certifiedMailSender

object Error {
  final case object SenderNotFound extends Exception(s"Email sender not found in configuration")
}
