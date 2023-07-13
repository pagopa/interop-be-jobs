package it.pagopa.interop.purposesarchiver.service

import akka.actor.typed.ActorSystem
import it.pagopa.interop.commons.jwt.service.InteropTokenGenerator
import it.pagopa.interop.commons.jwt.service.impl.DefaultInteropTokenGenerator
import it.pagopa.interop.commons.jwt.{JWTConfiguration, JWTInternalTokenConfig, KID, PrivateKeysKidHolder}
import it.pagopa.interop.commons.signer.service.SignerService
import it.pagopa.interop.commons.signer.service.impl.KMSSignerService
import it.pagopa.interop.purposesarchiver.service.{AgreementProcessInvoker, PurposeInvoker}
import it.pagopa.interop.purposesarchiver.service.impl.{AgreementProcessServiceImpl, PurposeProcessServiceImpl}
import it.pagopa.interop.purposesarchiver.system.ApplicationConfiguration
import it.pagopa.interop.agreementprocess.client.api.AgreementApi
import it.pagopa.interop.purposeprocess.client.api.PurposeApi
import it.pagopa.interop.commons.utils.TypeConversions.TryOps
import scala.util.control.NoStackTrace

import it.pagopa.interop.commons.utils.CORRELATION_ID_HEADER
import scala.concurrent.{ExecutionContextExecutor, Future}

import java.util.UUID
import scala.util.Try

trait Dependencies {

  implicit val contexts: Seq[(String, String)]   = (CORRELATION_ID_HEADER -> UUID.randomUUID().toString) :: Nil
  implicit val jwtConfig: JWTInternalTokenConfig = JWTConfiguration.jwtInternalTokenConfig

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

  private def signerService(implicit blockingEc: ExecutionContextExecutor): SignerService = new KMSSignerService(
    blockingEc
  )

  def generateBearer(implicit blockingEc: ExecutionContextExecutor): Future[String] = for {
    tokenGenerator <- interopTokenGenerator
    internalToken  <- tokenGenerator
      .generateInternalToken(
        subject = jwtConfig.subject,
        audience = jwtConfig.audience.toList,
        tokenIssuer = jwtConfig.issuer,
        secondsDuration = jwtConfig.durationInSeconds
      )
  } yield internalToken.serialized

  private def interopTokenGenerator(implicit blockingEc: ExecutionContextExecutor): Future[InteropTokenGenerator] = Try(
    new DefaultInteropTokenGenerator(
      signerService,
      new PrivateKeysKidHolder {
        override val RSAPrivateKeyset: Set[KID] = ApplicationConfiguration.rsaKeysIdentifiers
        override val ECPrivateKeyset: Set[KID]  = ApplicationConfiguration.ecKeysIdentifiers
      }
    )
  ).toFuture

  case class EventNotComplaint(clazz: String)
      extends Exception(s"Purposes archiver job failed because message is not compliant ${clazz}")
      with NoStackTrace
}
