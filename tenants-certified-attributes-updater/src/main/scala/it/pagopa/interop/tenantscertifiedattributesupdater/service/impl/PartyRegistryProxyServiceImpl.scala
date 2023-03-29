package it.pagopa.interop.tenantscertifiedattributesupdater.service.impl

import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.partyregistryproxy.client.api.InstitutionApi
import it.pagopa.interop.partyregistryproxy.client.invoker.BearerToken
import it.pagopa.interop.partyregistryproxy.client.model.Institutions
import it.pagopa.interop.tenantscertifiedattributesupdater.service.{
  PartyRegistryProxyInvoker,
  PartyRegistryProxyService
}

import java.util.UUID
import scala.concurrent.Future

final case class PartyRegistryProxyServiceImpl(invoker: PartyRegistryProxyInvoker, api: InstitutionApi)
    extends PartyRegistryProxyService {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  def getInstitutions(
    bearerToken: String
  )(page: Int, limit: Int)(implicit contexts: Seq[(String, String)]): Future[Institutions] = {
    val request =
      api.searchInstitutions(page = Some(page), limit = Some(limit), xCorrelationId = UUID.randomUUID().toString)(
        BearerToken(bearerToken)
      )
    invoker.invoke(request, s"Getting institutions page=${page.toString}/limit=${limit.toString}")
  }
}
