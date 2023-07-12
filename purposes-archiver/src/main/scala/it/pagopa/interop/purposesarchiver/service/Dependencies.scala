package it.pagopa.interop.purposesarchiver.service

import akka.actor.typed.ActorSystem
import it.pagopa.interop.purposesarchiver.service.{AgreementProcessInvoker, PurposeInvoker}
import it.pagopa.interop.purposesarchiver.service.impl.{AgreementProcessServiceImpl, PurposeProcessServiceImpl}
import it.pagopa.interop.purposesarchiver.system.ApplicationConfiguration
import it.pagopa.interop.agreementprocess.client.api.AgreementApi
import it.pagopa.interop.purposeprocess.client.api.PurposeApi

import scala.concurrent.ExecutionContextExecutor
import it.pagopa.interop.commons.utils.CORRELATION_ID_HEADER

import java.util.UUID

trait Dependencies {

  implicit val contexts: Seq[(String, String)] = (CORRELATION_ID_HEADER -> UUID.randomUUID().toString) :: Nil

  def agreementProcessService(implicit
    blockingEc: ExecutionContextExecutor,
    actorSystem: ActorSystem[_]
  ): AgreementProcessServiceImpl =
    AgreementProcessServiceImpl(
      AgreementProcessInvoker(blockingEc)(actorSystem.classicSystem),
      AgreementApi(ApplicationConfiguration.agreementProcessURL)
    )

  def purposeProcessService(implicit
    blockingEc: ExecutionContextExecutor,
    actorSystem: ActorSystem[_]
  ): PurposeProcessServiceImpl =
    PurposeProcessServiceImpl(
      PurposeInvoker(blockingEc)(actorSystem.classicSystem),
      PurposeApi(ApplicationConfiguration.purposeProcessURL)
    )

}
