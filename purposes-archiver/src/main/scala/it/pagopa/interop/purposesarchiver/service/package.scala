package it.pagopa.interop.purposesarchiver

import akka.actor.ActorSystem
import it.pagopa.interop.agreementprocess.{client => AgreementClient}
import it.pagopa.interop.purposeprocess.{client => PurposeClient}

import scala.concurrent.ExecutionContextExecutor

package object service {

  type AgreementProcessInvoker = AgreementClient.invoker.ApiInvoker
  type PurposeInvoker          = PurposeClient.invoker.ApiInvoker

  object AgreementProcessInvoker {
    def apply(blockingEc: ExecutionContextExecutor)(implicit actorSystem: ActorSystem): AgreementProcessInvoker =
      AgreementClient.invoker
        .ApiInvoker(AgreementClient.api.EnumsSerializers.all, blockingEc)
  }

  object PurposeInvoker {
    def apply(blockingEc: ExecutionContextExecutor)(implicit actorSystem: ActorSystem): PurposeInvoker =
      PurposeClient.invoker.ApiInvoker(PurposeClient.api.EnumsSerializers.all, blockingEc)
  }

}
