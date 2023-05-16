package it.pagopa.interop.tenantsattributeschecker

import akka.actor.typed.ActorSystem
import it.pagopa.interop.agreementprocess.client.api.AgreementApi
import it.pagopa.interop.commons.utils.CORRELATION_ID_HEADER
import it.pagopa.interop.tenantsattributeschecker.service.impl.AgreementProcessServiceImpl
import it.pagopa.interop.tenantsattributeschecker.service.AgreementProcessInvoker

import java.util.UUID
import scala.concurrent.ExecutionContextExecutor

trait Dependencies {

  implicit val contexts: Seq[(String, String)] = (CORRELATION_ID_HEADER -> UUID.randomUUID().toString) :: Nil

//  def agreementProcessService(
//    blockingEc: ExecutionContextExecutor
//  )(implicit actorSystem: ActorSystem[_]): AgreementProcessServiceImpl =
//    AgreementProcessServiceImpl(
//      AgreementProcessInvoker(blockingEc)(actorSystem.classicSystem),
//      AgreementApi(ApplicationConfiguration.agreementProcessURL)
//    )(blockingEc)
  def agreementProcessService(
    blockingEc: ExecutionContextExecutor
  )(implicit actorSystem: ActorSystem[_]): AgreementProcessServiceImpl =
    AgreementProcessServiceImpl(
      AgreementProcessInvoker(blockingEc)(actorSystem.classicSystem),
      AgreementApi(ApplicationConfiguration.agreementProcessURL)
    )
}
