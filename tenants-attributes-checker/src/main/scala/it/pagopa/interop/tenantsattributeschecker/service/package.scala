//package it.pagopa.interop.tenantsattributeschecker
//
//import akka.actor.ActorSystem
//import it.pagopa.interop.agreementprocess
//import scala.concurrent.ExecutionContextExecutor
//
//package object service {
//  type AgreementProcessInvoker = agreementprocess.client.invoker.ApiInvoker
//  type TenantProcessInvoker    = tenantprocess.client.invoker.ApiInvoker
//
//  object TenantProcessInvoker {
//    def apply(blockingEc: ExecutionContextExecutor)(implicit actorSystem: ActorSystem): TenantProcessInvoker =
//      tenantprocess.client.invoker.ApiInvoker(tenantprocess.client.api.EnumsSerializers.all, blockingEc)
//  }
//
//  object AgreementProcessInvoker {
//    def apply(blockingEc: ExecutionContextExecutor)(implicit actorSystem: ActorSystem): AgreementProcessInvoker =
//      agreementprocess.client.invoker.ApiInvoker(agreementprocess.client.api.EnumsSerializers.all, blockingEc)
//  }
//
//}
