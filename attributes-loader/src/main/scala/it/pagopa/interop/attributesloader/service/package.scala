package it.pagopa.interop.attributesloader

import akka.actor.ActorSystem
import it.pagopa.interop._
import scala.concurrent.ExecutionContextExecutor

package object service {
  type AttributeRegistryProcessInvoker = attributeregistryprocess.client.invoker.ApiInvoker

  object AttributeRegistryProcessInvoker {
    def apply(
      blockingEc: ExecutionContextExecutor
    )(implicit actorSystem: ActorSystem): AttributeRegistryProcessInvoker =
      attributeregistryprocess.client.invoker
        .ApiInvoker(attributeregistryprocess.client.api.EnumsSerializers.all, blockingEc)
  }

}
