package it.pagopa.interop.attributesloader

import akka.actor.typed.ActorSystem
import it.pagopa.interop.attributeregistryprocess.client.api.AttributeApi
import it.pagopa.interop.attributesloader.service.impl.{AttributeRegistryProcessServiceImpl, PartyRegistryServiceImpl}
import it.pagopa.interop.attributesloader.service.{AttributeRegistryProcessInvoker, PartyRegistryInvoker}
import it.pagopa.interop.attributesloader.system.ApplicationConfiguration
import it.pagopa.interop.commons.cqrs.service.{MongoDbReadModelService, ReadModelService}
import it.pagopa.interop.commons.jwt.service.InteropTokenGenerator
import it.pagopa.interop.commons.jwt.service.impl.DefaultInteropTokenGenerator
import it.pagopa.interop.commons.jwt.{JWTConfiguration, JWTInternalTokenConfig, KID, PrivateKeysKidHolder}
import it.pagopa.interop.commons.signer.service.SignerService
import it.pagopa.interop.commons.signer.service.impl.KMSSignerService
import it.pagopa.interop.commons.utils.CORRELATION_ID_HEADER
import it.pagopa.interop.commons.utils.TypeConversions.TryOps
import it.pagopa.interop.partyregistryproxy.client.api.{CategoryApi, InstitutionApi}

import java.util.UUID
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.Try

trait Dependencies {

  implicit val contexts: Seq[(String, String)] = (CORRELATION_ID_HEADER -> UUID.randomUUID().toString) :: Nil

  val jwtConfig: JWTInternalTokenConfig = JWTConfiguration.jwtInternalTokenConfig

  def attributeRegistryProcessService(
    blockingEc: ExecutionContextExecutor
  )(implicit actorSystem: ActorSystem[_]): AttributeRegistryProcessServiceImpl =
    AttributeRegistryProcessServiceImpl(
      AttributeRegistryProcessInvoker(blockingEc)(actorSystem.classicSystem),
      AttributeApi(ApplicationConfiguration.attributeRegistryProcessURL)
    )

  def partyRegistryService(
    blockingEc: ExecutionContextExecutor
  )(implicit actorSystem: ActorSystem[_]): PartyRegistryServiceImpl =
    PartyRegistryServiceImpl(
      PartyRegistryInvoker(blockingEc)(actorSystem.classicSystem),
      CategoryApi(ApplicationConfiguration.partyRegistryProxyURL),
      InstitutionApi(ApplicationConfiguration.partyRegistryProxyURL)
    )

  def readModelService: ReadModelService = new MongoDbReadModelService(ApplicationConfiguration.readModelConfig)

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
