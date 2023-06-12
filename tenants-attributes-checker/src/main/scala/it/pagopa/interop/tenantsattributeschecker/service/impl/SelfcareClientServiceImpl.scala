package it.pagopa.interop.tenantsattributeschecker.service.impl

import akka.actor.typed.ActorSystem
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.TypeConversions._
import it.pagopa.interop.selfcare.v2.client.api.{EnumsSerializers, InstitutionsApi}
import it.pagopa.interop.selfcare.v2.client.invoker.{ApiInvoker, ApiKeyValue}
import it.pagopa.interop.selfcare.v2.client.model.Institution
import it.pagopa.interop.tenantsattributeschecker.service.SelfcareClientService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SelfcareClientServiceImpl(selfcareClientServiceURL: String, selfcareClientApiKey: String)(implicit
  system: ActorSystem[_]
) extends SelfcareClientService {

  implicit val apiKeyValue: ApiKeyValue = ApiKeyValue(selfcareClientApiKey)
  val invoker: ApiInvoker               = ApiInvoker(EnumsSerializers.all)(system.classicSystem)
  val institutionsApi: InstitutionsApi  = InstitutionsApi(selfcareClientServiceURL)

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  override def getInstitution(selfcareId: String)(implicit contexts: Seq[(String, String)]): Future[Institution] =
    selfcareId.toFutureUUID.flatMap { selfcareUUID =>
      val request = institutionsApi.getInstitution(selfcareUUID)
      invoker.invoke(request, s"Institution $selfcareId")
    }
}
