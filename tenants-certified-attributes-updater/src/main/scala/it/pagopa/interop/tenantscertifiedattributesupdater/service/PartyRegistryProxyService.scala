package it.pagopa.interop.tenantscertifiedattributesupdater.service

import it.pagopa.interop.partyregistryproxy.client.model.Institutions

import scala.concurrent.Future

trait PartyRegistryProxyService {
  def getInstitutions(bearerToken: String)(page: Int, limit: Int)(implicit
    contexts: Seq[(String, String)]
  ): Future[Institutions]
}
