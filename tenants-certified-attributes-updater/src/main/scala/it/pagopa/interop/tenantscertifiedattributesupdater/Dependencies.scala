package it.pagopa.interop.tenantscertifiedattributesupdater

import akka.actor.typed.ActorSystem
import it.pagopa.interop.commons.jwt.{JWTConfiguration, JWTInternalTokenConfig}
import it.pagopa.interop.commons.signer.service.SignerService
import it.pagopa.interop.commons.signer.service.impl.KMSSignerService
import it.pagopa.interop.partyregistryproxy.client.api.InstitutionApi
import it.pagopa.interop.tenantprocess.client.api.TenantApi
import it.pagopa.interop.tenantscertifiedattributesupdater.service.impl.{
  PartyRegistryProxyServiceImpl,
  TenantProcessServiceImpl
}
import it.pagopa.interop.tenantscertifiedattributesupdater.service.{PartyRegistryProxyInvoker, TenantProcessInvoker}
import it.pagopa.interop.tenantscertifiedattributesupdater.system.ApplicationConfiguration

import scala.concurrent.ExecutionContextExecutor
import it.pagopa.interop.commons.utils.CORRELATION_ID_HEADER
import java.util.UUID

trait Dependencies {

  implicit val contexts: Seq[(String, String)] = (CORRELATION_ID_HEADER -> UUID.randomUUID().toString()) :: Nil

  val jwtConfig: JWTInternalTokenConfig = JWTConfiguration.jwtInternalTokenConfig

  def partyRegistryProxyService(
    blockingEc: ExecutionContextExecutor
  )(implicit actorSystem: ActorSystem[_]): PartyRegistryProxyServiceImpl = PartyRegistryProxyServiceImpl(
    PartyRegistryProxyInvoker(blockingEc)(actorSystem.classicSystem),
    InstitutionApi(ApplicationConfiguration.partyRegistryProxyURL)
  )

  def tenantProcessService(
    blockingEc: ExecutionContextExecutor
  )(implicit actorSystem: ActorSystem[_]): TenantProcessServiceImpl =
    TenantProcessServiceImpl(
      TenantProcessInvoker(blockingEc)(actorSystem.classicSystem),
      TenantApi(ApplicationConfiguration.tenantProcessURL)
    )(blockingEc)

  def signerService(blockingEc: ExecutionContextExecutor): SignerService = new KMSSignerService(blockingEc)

}
