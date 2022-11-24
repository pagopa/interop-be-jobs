package it.pagopa.interop.tenantscertifiedattributesupdater

import akka.actor.typed.ActorSystem
import it.pagopa.interop.commons.jwt.{JWTConfiguration, JWTInternalTokenConfig}
import it.pagopa.interop.commons.signer.service.SignerService
import it.pagopa.interop.commons.signer.service.impl.KMSSignerService
import it.pagopa.interop.partyregistryproxy.client.api.InstitutionApi
import it.pagopa.interop.tenantprocess.client.api.TenantApi
import it.pagopa.interop.tenantmanagement.client.api.{TenantApi => TenantManagementApi}
import it.pagopa.interop.tenantscertifiedattributesupdater.service.impl.{
  PartyRegistryProxyServiceImpl,
  TenantManagementServiceImpl,
  TenantProcessServiceImpl
}
import it.pagopa.interop.tenantscertifiedattributesupdater.service.{
  PartyRegistryProxyInvoker,
  TenantManagementInvoker,
  TenantProcessInvoker
}
import it.pagopa.interop.tenantscertifiedattributesupdater.system.ApplicationConfiguration

import scala.concurrent.ExecutionContextExecutor

trait Dependencies {

  implicit val contexts: Seq[(String, String)] = Seq.empty

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

  def tenantManagementService(
    blockingEc: ExecutionContextExecutor
  )(implicit actorSystem: ActorSystem[_]): TenantManagementServiceImpl =
    TenantManagementServiceImpl(
      TenantManagementInvoker(blockingEc)(actorSystem.classicSystem),
      TenantManagementApi(ApplicationConfiguration.tenantManagementURL)
    )(blockingEc)

  def signerService(blockingEc: ExecutionContextExecutor): SignerService = new KMSSignerService(blockingEc)

}
