package it.pagopa.interop.attributesloader

import akka.actor.ActorSystem
import it.pagopa.interop._

package object service {
  type AttributeRegistryManagementInvoker = attributeregistrymanagement.client.invoker.ApiInvoker

  object AttributeRegistryManagementInvoker {
    def apply()(implicit actorSystem: ActorSystem): AttributeRegistryManagementInvoker =
      attributeregistrymanagement.client.invoker.ApiInvoker(attributeregistrymanagement.client.api.EnumsSerializers.all)
  }

}
