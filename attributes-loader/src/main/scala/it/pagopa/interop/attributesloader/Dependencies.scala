package it.pagopa.interop.attributesloader

import akka.actor.typed.ActorSystem
import it.pagopa.interop.attributeregistrymanagement.client.api.AttributeApi
import it.pagopa.interop.attributesloader.service.AttributeRegistryManagementInvoker
import it.pagopa.interop.attributesloader.service.impl.AttributeRegistryManagementServiceImpl
import it.pagopa.interop.attributesloader.system.ApplicationConfiguration
import it.pagopa.interop.commons.jwt.service.InteropTokenGenerator
import it.pagopa.interop.commons.jwt.service.impl.DefaultInteropTokenGenerator
import it.pagopa.interop.commons.jwt.{JWTConfiguration, JWTInternalTokenConfig, KID, PrivateKeysKidHolder}
import it.pagopa.interop.commons.signer.service.SignerService
import it.pagopa.interop.commons.signer.service.impl.KMSSignerService
import it.pagopa.interop.commons.utils.TypeConversions.TryOps

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.concurrent.ExecutionContextExecutor

trait Dependencies {

  implicit val contexts: Seq[(String, String)] = Seq.empty

  val jwtConfig: JWTInternalTokenConfig = JWTConfiguration.jwtInternalTokenConfig

  def attributeRegistryManagementService(
    blockingEc: ExecutionContextExecutor
  )(implicit actorSystem: ActorSystem[_], ec: ExecutionContext): AttributeRegistryManagementServiceImpl =
    AttributeRegistryManagementServiceImpl(
      AttributeRegistryManagementInvoker(blockingEc)(actorSystem.classicSystem),
      AttributeApi(ApplicationConfiguration.attributeRegistryManagementURL)
    )

  def signerService(implicit blockingEc: ExecutionContextExecutor): SignerService = new KMSSignerService(blockingEc)

  def interopTokenGenerator(
    blockingEc: ExecutionContextExecutor
  )(implicit actorSystem: ActorSystem[_], ec: ExecutionContext): Future[InteropTokenGenerator] =
    Try(
      new DefaultInteropTokenGenerator(
        signerService(blockingEc),
        new PrivateKeysKidHolder {
          override val RSAPrivateKeyset: Set[KID] = ApplicationConfiguration.rsaKeysIdentifiers
          override val ECPrivateKeyset: Set[KID]  = ApplicationConfiguration.ecKeysIdentifiers
        }
      )
    ).toFuture
}
