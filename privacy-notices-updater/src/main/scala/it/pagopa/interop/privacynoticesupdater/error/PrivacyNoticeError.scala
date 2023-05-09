package it.pagopa.interop.privacynoticesupdater.error

import it.pagopa.interop.commons.utils.errors.ComponentError
import sttp.model.StatusCode
import it.pagopa.interop.privacynoticesupdater.model.http._

object PrivacyNoticeError {
  final case class DynamoReadingError(message: String)
      extends ComponentError("0001", s"Error while reading data from Dynamo -> $message")
  final case class OneTrustHttpError(statusCode: StatusCode)
      extends ComponentError("0002", s"Error while reading data from One Trust with status code $statusCode")
  final case class OneTrustAuthError(error: OneTrustAuthenticationError, statusCode: StatusCode)
      extends ComponentError(
        "0003",
        s"Error while reading data from One Trust with message ${error.error}, description${error.error_description} status code $statusCode"
      )
  final case class OneTrustDeserializationError(body: String, error: Exception)
      extends ComponentError(
        "0004",
        s"Error while reading data from One Trust -> $body and error ${error.getMessage()}"
      )
}
