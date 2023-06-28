package it.pagopa.interop.attributesloader

import akka.actor.typed.ActorSystem
import it.pagopa.interop.attributesloader.system.ApplicationConfiguration
import it.pagopa.interop.attributesloader.service.impl.CatalogManagementServiceImpl
import it.pagopa.interop.attributesloader.service.CatalogManagementInvoker
import it.pagopa.interop.catalogmanagement.client.api.EServiceApi
import it.pagopa.interop.commons.jwt.service.InteropTokenGenerator
import it.pagopa.interop.commons.jwt.service.impl.DefaultInteropTokenGenerator
import it.pagopa.interop.commons.jwt.{JWTConfiguration, JWTInternalTokenConfig, KID, PrivateKeysKidHolder}
import it.pagopa.interop.commons.signer.service.SignerService
import it.pagopa.interop.commons.signer.service.impl.KMSSignerService
import it.pagopa.interop.commons.utils.TypeConversions.TryOps

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.concurrent.ExecutionContextExecutor
import it.pagopa.interop.commons.utils.CORRELATION_ID_HEADER
import java.util.UUID

trait Dependencies {

  implicit val contexts: Seq[(String, String)] = (CORRELATION_ID_HEADER -> UUID.randomUUID().toString()) :: Nil

  val jwtConfig: JWTInternalTokenConfig = JWTConfiguration.jwtInternalTokenConfig

  def catalogManagementProcessService(
    blockingEc: ExecutionContextExecutor
  )(implicit actorSystem: ActorSystem[_]): CatalogManagementServiceImpl = CatalogManagementServiceImpl(
    CatalogManagementInvoker(blockingEc)(actorSystem.classicSystem),
    EServiceApi(ApplicationConfiguration.catalogManagementURL)
  )

  def signerService(implicit blockingEc: ExecutionContextExecutor): SignerService = new KMSSignerService(blockingEc)

  def interopTokenGenerator(
    blockingEc: ExecutionContextExecutor
  )(implicit ec: ExecutionContext): Future[InteropTokenGenerator] = Try(
    new DefaultInteropTokenGenerator(
      signerService(blockingEc),
      new PrivateKeysKidHolder {
        override val RSAPrivateKeyset: Set[KID] = ApplicationConfiguration.rsaKeysIdentifiers
        override val ECPrivateKeyset: Set[KID]  = ApplicationConfiguration.ecKeysIdentifiers
      }
    )
  ).toFuture
}
