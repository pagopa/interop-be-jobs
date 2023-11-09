package it.pagopa.interop.dashboardmetricsreportgenerator

import cats.syntax.all._
import it.pagopa.interop.selfcare.v2.client.api.{EnumsSerializers, InstitutionsApi}
import it.pagopa.interop.selfcare.v2.client.invoker._
import akka.actor.ActorSystem
import scala.concurrent.Future
import java.util.UUID
import it.pagopa.interop.selfcare.v2.client.model._
import scala.concurrent.ExecutionContext
import scala.util.control.NoStackTrace
import java.time.OffsetDateTime

class SelfcareV2Client(config: SelfcareV2ClientConfiguration) {

  private val actorSystem: ActorSystem                  = ActorSystem("selfcareV2ClientActorSystem")
  implicit val selfcareV2ClientApiKeyValue: ApiKeyValue = ApiKeyValue(config.apiKey)
  private val invoker: ApiInvoker                       = ApiInvoker(EnumsSerializers.all)(actorSystem.classicSystem)
  private val institutionsApi: InstitutionsApi          = InstitutionsApi(config.url)

  def getOnboardingDate(selfcareId: UUID): Future[OffsetDateTime] = {
    val request: ApiRequest[OnboardingsResponse] = institutionsApi.getOnboardingsInstitutionUsingGET(
      institutionId = selfcareId.toString,
      productId = config.interopProductName.some
    )
    implicit val ec: ExecutionContext            = actorSystem.dispatcher
    invoker
      .execute(request)
      .recoverWith {
        case ApiError(code, _, _, _, _) if code == 404 => Future.failed(InstitutionNotFound(selfcareId))
      }
      .flatMap(
        _.content.onboardings
          .map(_.flatMap(_.createdAt).minOption)
          .fold[Future[OffsetDateTime]](Future.failed(OnboardingDateNotFound(selfcareId))) {
            case Some(value) => Future.successful(value)
            case _           => Future.failed(OnboardingDateNotFound(selfcareId))
          }
      )
  }

  case class InstitutionNotFound(selfcareId: UUID)
      extends Exception(s"Unable to get the institution ${selfcareId.toString}")
      with NoStackTrace

  case class OnboardingDateNotFound(selfcareId: UUID)
      extends Exception(s"Unable to get the onboarding date for ${selfcareId.toString}")
      with NoStackTrace

  def close(): Future[Unit] = actorSystem.terminate().map(_ => ())(actorSystem.dispatcher)
}
