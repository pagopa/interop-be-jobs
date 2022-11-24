package it.pagopa.interop.tenantscertifiedattributesupdater

import akka.actor.ActorSystem
import it.pagopa.interop._

import scala.concurrent.ExecutionContextExecutor

package object service {
  type PartyRegistryProxyInvoker = partyregistryproxy.client.invoker.ApiInvoker
  type TenantProcessInvoker      = tenantprocess.client.invoker.ApiInvoker
  type TenantManagementInvoker   = tenantmanagement.client.invoker.ApiInvoker

  object PartyRegistryProxyInvoker {
    def apply(blockingEc: ExecutionContextExecutor)(implicit actorSystem: ActorSystem): PartyRegistryProxyInvoker =
      partyregistryproxy.client.invoker.ApiInvoker(partyregistryproxy.client.api.EnumsSerializers.all, blockingEc)
  }

  object TenantProcessInvoker {
    def apply(blockingEc: ExecutionContextExecutor)(implicit actorSystem: ActorSystem): TenantProcessInvoker =
      tenantprocess.client.invoker.ApiInvoker(tenantprocess.client.api.EnumsSerializers.all, blockingEc)
  }

  object TenantManagementInvoker {
    def apply(blockingEc: ExecutionContextExecutor)(implicit actorSystem: ActorSystem): TenantManagementInvoker =
      tenantmanagement.client.invoker.ApiInvoker(tenantmanagement.client.api.EnumsSerializers.all, blockingEc)
  }

}
