package it.pagopa.interop.attributesloader.service.impl

import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.attributesloader.service.PartyRegistryService
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.withHeaders
import it.pagopa.interop.partyregistryproxy.client.api.{CategoryApi, InstitutionApi}
import it.pagopa.interop.partyregistryproxy.client.invoker.{ApiRequest, BearerToken, ApiInvoker => PartyProxyInvoker}
import it.pagopa.interop.partyregistryproxy.client.model.{Categories, Institutions}

import scala.concurrent.Future

final case class PartyRegistryServiceImpl(
  invoker: PartyProxyInvoker,
  categoryApi: CategoryApi,
  institutionApi: InstitutionApi
) extends PartyRegistryService {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  override def getCategories(page: Option[Int] = None, limit: Option[Int] = None)(implicit
    contexts: Seq[(String, String)]
  ): Future[Categories] =
    withHeaders { (bearerToken, correlationId) =>
      val request: ApiRequest[Categories] =
        categoryApi.getCategories(origin = None, xCorrelationId = correlationId, page = page, limit = limit)(
          BearerToken(bearerToken)
        )
      invoker.invoke(request, "Retrieving categories")
    }

  override def getInstitutions(page: Option[Int] = None, limit: Option[Int] = None)(implicit
    contexts: Seq[(String, String)]
  ): Future[Institutions] =
    withHeaders { (bearerToken, correlationId) =>
      val request: ApiRequest[Institutions] =
        institutionApi.searchInstitutions(xCorrelationId = correlationId, page = page, limit = limit)(
          BearerToken(bearerToken)
        )
      invoker.invoke(request, "Retrieving Institutions")
    }
}
