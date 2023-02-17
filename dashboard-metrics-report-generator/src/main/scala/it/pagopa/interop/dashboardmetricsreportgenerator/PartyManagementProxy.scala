package it.pagopa.interop.dashboardmetricsreportgenerator

import cats.syntax.all._
import it.pagopa.interop.selfcare.partymanagement.client.api._
import it.pagopa.interop.selfcare.partymanagement.client.invoker._
import akka.actor.ActorSystem
import scala.concurrent.Future
import java.util.UUID
import it.pagopa.interop.selfcare.partymanagement.client.model._
import scala.concurrent.ExecutionContext
import scala.util.control.NoStackTrace
import java.time.OffsetDateTime

class PartyManagementProxy(config: PartyManagementConfiguration) {

  private val actorSystem: ActorSystem                = ActorSystem("partyManagementActorSystem")
  private val partyManagementApiKeyValue: ApiKeyValue = ApiKeyValue(config.partyManagementApiKey)
  private val invoker: ApiInvoker                     = ApiInvoker(EnumsSerializers.all)(actorSystem.classicSystem)
  private val api: PartyApi                           = PartyApi(config.partyManagementUrl)

  def getOnboardingDate(selfcareId: UUID): Future[OffsetDateTime] = {
    val request: ApiRequest[Relationships] = api.getRelationships(
      from = None,
      to = selfcareId.some,
      // If there's a manager the onboarding is completed for sure
      roles = PartyRole.MANAGER :: Nil,
      // All but PENDING
      states = List(
        RelationshipState.ACTIVE,
        RelationshipState.DELETED,
        RelationshipState.REJECTED,
        RelationshipState.SUSPENDED
      ),
      products = config.interopProductName :: Nil,
      productRoles = Nil
    )("dashboard-metrics-job-uid")(partyManagementApiKeyValue)
    implicit val ec: ExecutionContext      = actorSystem.dispatcher
    invoker
      .execute(request)
      .recoverWith {
        case ApiError(code, _, _, _, _) if code == 404 => Future.failed(InstitutionNotFound(selfcareId))
      }
      .flatMap(
        _.content.items
          .map(_.product.createdAt)
          .minOption
          .fold[Future[OffsetDateTime]](Future.failed(OnboardingDateNotFound(selfcareId)))(Future.successful)
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
