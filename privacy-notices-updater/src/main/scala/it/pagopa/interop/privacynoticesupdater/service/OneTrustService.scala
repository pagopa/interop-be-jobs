package it.pagopa.interop.privacynoticesupdater.service

import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.privacynoticesupdater.util.OneTrustConfiguration
import it.pagopa.interop.privacynoticesupdater.error.PrivacyNoticeError._
import it.pagopa.interop.privacynoticesupdater.model.http._
import it.pagopa.interop.privacynoticesupdater.model.http.ModelFormats._
import it.pagopa.interop.privacynoticesupdater.model.http.ErrorFormats._
import sttp.client4._
import sttp.model._
import sttp.client4.circe._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import java.util.UUID
import java.time.{OffsetDateTime, ZoneId}
import java.util.concurrent.TimeUnit
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter

trait OneTrustService {
  def getBearerToken(): Future[Authorization]

  def getById(id: UUID)(bearer: String): Future[Option[PrivacyNotice]]

}

final class OneTrustServiceImpl(config: OneTrustConfiguration)(implicit
  ec: ExecutionContext,
  backend: WebSocketBackend[Future],
  contexts: Seq[(String, String)]
) extends OneTrustService {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  val timeout = FiniteDuration(config.readTimeoutInSeconds, TimeUnit.SECONDS)

  override def getById(id: UUID)(bearer: String): Future[Option[PrivacyNotice]] = {
    logger.debug(s"Get Privacy notice with id $id from One Trust")

    val now: OffsetDateTime = OffsetDateTime.now(ZoneId.of("Europe/Rome"))

    val url: String =
      s"${config.privacyNoticesUrl}/${id}?date=${now.truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}"

    logger.debug(s"Url One Trust to call is $url")

    val sttpRequest = basicRequest.auth
      .bearer(bearer)
      .readTimeout(timeout)
      .header(Header.accept(MediaType.ApplicationJson))
      .method(Method.GET, uri"${url}")
      .response(asJsonEither[Option[ErrorMessage], PrivacyNotice])
      .send(backend)

    sttpRequest.flatMap { response =>
      {
        response.body match {
          case Right(success) => Future.successful(Some(success))
          case Left(error)    =>
            error match {
              case HttpError(body, StatusCode.NotFound) => {
                logger.info(s"Receiving Not Found message")
                if (body.map(_.path).isDefined)(Future.failed(OneTrustHttpError(StatusCode.NotFound)))
                else (Future.successful(None))
              }
              case HttpError(_, statusCode)             => Future.failed(OneTrustHttpError(statusCode))
              case DeserializationException(_, error)   => Future.failed(OneTrustDeserializationError(error))
            }
        }
      }
    }
  }

  override def getBearerToken(): Future[Authorization] = {
    logger.debug(s"Get bearer token from One Trust")

    val sttpRequest = basicRequest
      .readTimeout(timeout)
      .header(Header.contentType(MediaType.MultipartFormData))
      .header(Header.accept(MediaType.ApplicationJson))
      .multipartBody(
        multipart("grant_type", "client_credentials"),
        multipart("client_id", config.clientId),
        multipart("client_secret", config.clientSecret)
      )
      .method(Method.POST, uri"${config.tokenUrl}")
      .response(asJsonEither[OneTrustAuthenticationError, Authorization])
      .send(backend)

    sttpRequest.flatMap { response =>
      response.body match {
        case Right(success) => Future.successful(success)
        case Left(error)    =>
          error match {
            case HttpError(body, statusCode)        => Future.failed(OneTrustAuthError(body, statusCode))
            case DeserializationException(_, error) => Future.failed(OneTrustDeserializationError(error))
          }
      }
    }
  }
}
