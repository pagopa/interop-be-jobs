package it.pagopa.interop.tenantscertifiedattributesupdater.service.impl

import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.partyregistryproxy.client.api.{AooApi, InstitutionApi, UoApi}
import it.pagopa.interop.partyregistryproxy.client.invoker.BearerToken
import it.pagopa.interop.partyregistryproxy.client.model.Institutions
import it.pagopa.interop.tenantscertifiedattributesupdater.service.{
  PartyRegistryProxyInvoker,
  PartyRegistryProxyService
}

import java.util.UUID
import scala.concurrent.Future

final case class PartyRegistryProxyServiceImpl(
  invoker: PartyRegistryProxyInvoker,
  institutionApi: InstitutionApi,
  aooApi: AooApi,
  uoApi: UoApi
) extends PartyRegistryProxyService {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  def getInstitutions(
    bearerToken: String
  )(page: Int, limit: Int)(implicit contexts: Seq[(String, String)]): Future[Institutions] = {
    val request =
      institutionApi.searchInstitutions(
        page = Some(page),
        limit = Some(limit),
        xCorrelationId = UUID.randomUUID().toString
      )(BearerToken(bearerToken))
    invoker.invoke(request, s"Getting institutions page=${page.toString}/limit=${limit.toString}")
  }

  override def getAOO(
    bearerToken: String
  )(page: Int, limit: Int)(implicit contexts: Seq[(String, String)]): Future[Institutions] = {
    val request =
      aooApi.searchAOO(page = Some(page), limit = Some(limit), xCorrelationId = UUID.randomUUID().toString)(
        BearerToken(bearerToken)
      )
    invoker.invoke(request, s"Getting AOO page=${page.toString}/limit=${limit.toString}")
  }

  override def getUO(
    bearerToken: String
  )(page: Int, limit: Int)(implicit contexts: Seq[(String, String)]): Future[Institutions] = {
    val request =
      uoApi.searchUO(page = Some(page), limit = Some(limit), xCorrelationId = UUID.randomUUID().toString)(
        BearerToken(bearerToken)
      )
    invoker.invoke(request, s"Getting UO page=${page.toString}/limit=${limit.toString}")
  }
}
