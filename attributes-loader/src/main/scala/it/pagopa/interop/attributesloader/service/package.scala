package it.pagopa.interop.attributesloader

import akka.actor.ActorSystem
import it.pagopa.interop._
import scala.concurrent.ExecutionContextExecutor

package object service {
  type CatalogManagementInvoker = catalogmanagement.client.invoker.ApiInvoker

  object CatalogManagementInvoker {
    def apply(blockingEc: ExecutionContextExecutor)(implicit actorSystem: ActorSystem): CatalogManagementInvoker =
      catalogmanagement.client.invoker
        .ApiInvoker(catalogmanagement.client.api.EnumsSerializers.all, blockingEc)
  }

}
