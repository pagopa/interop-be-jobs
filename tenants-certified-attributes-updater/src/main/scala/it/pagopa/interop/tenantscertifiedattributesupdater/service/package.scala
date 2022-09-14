package it.pagopa.interop.tenantscertifiedattributesupdater

import akka.actor.ActorSystem
import it.pagopa.interop.{partyregistryproxy, tenantprocess}

import scala.concurrent.ExecutionContextExecutor

package object service {
  type PartyRegistryProxyInvoker = partyregistryproxy.client.invoker.ApiInvoker
  type TenantProcessInvoker      = tenantprocess.client.invoker.ApiInvoker

  object PartyRegistryProxyInvoker {
    def apply(blockingEc: ExecutionContextExecutor)(implicit actorSystem: ActorSystem): PartyRegistryProxyInvoker =
      partyregistryproxy.client.invoker.ApiInvoker(partyregistryproxy.client.api.EnumsSerializers.all, blockingEc)
  }

  object TenantProcessInvoker {
    def apply(blockingEc: ExecutionContextExecutor)(implicit actorSystem: ActorSystem): TenantProcessInvoker =
      tenantprocess.client.invoker.ApiInvoker(tenantprocess.client.api.EnumsSerializers.all, blockingEc)
  }

}
