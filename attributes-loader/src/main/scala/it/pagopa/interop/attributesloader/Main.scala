package it.pagopa.interop.attributesloader

import akka.actor.CoordinatedShutdown
import it.pagopa.interop.attributeregistrymanagement.client.api.AttributeApi
import it.pagopa.interop.attributesloader.service.AttributeRegistryManagementInvoker
import it.pagopa.interop.attributesloader.service.impl.AttributeRegistryManagementServiceImpl
import it.pagopa.interop.attributesloader.system.{ApplicationConfiguration, classicActorSystem, executionContext}
import it.pagopa.interop.commons.jwt._
import it.pagopa.interop.commons.jwt.service.InteropTokenGenerator
import it.pagopa.interop.commons.jwt.service.impl.DefaultInteropTokenGenerator
import it.pagopa.interop.commons.utils.CORRELATION_ID_HEADER
import it.pagopa.interop.commons.utils.TypeConversions.TryOps
import it.pagopa.interop.commons.vault.VaultClientConfiguration
import it.pagopa.interop.commons.vault.service.VaultTransitService
import it.pagopa.interop.commons.vault.service.impl.VaultTransitServiceImpl
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

//shuts down the actor system in case of startup errors
case object ErrorShutdown   extends CoordinatedShutdown.Reason
case object SuccessShutdown extends CoordinatedShutdown.Reason

trait AttributeRegistryManagementDependency {
  val attributeRegistryManagementApi: AttributeApi = AttributeApi(
    ApplicationConfiguration.attributeRegistryManagementURL
  )

  val attributeRegistryManagementService: AttributeRegistryManagementServiceImpl =
    AttributeRegistryManagementServiceImpl(AttributeRegistryManagementInvoker(), attributeRegistryManagementApi)
}

object Main extends App with AttributeRegistryManagementDependency {

  implicit val logger: Logger = LoggerFactory.getLogger(this.getClass)

  logger.info("Loading attributes data...")

  lazy val jwtConfig: JWTInternalTokenConfig = JWTConfiguration.jwtInternalTokenConfig
  val vaultService: VaultTransitService      = new VaultTransitServiceImpl(VaultClientConfiguration.vaultConfig)

  val interopTokenGenerator: Try[InteropTokenGenerator] =
    Try(
      new DefaultInteropTokenGenerator(
        vaultService,
        new PrivateKeysKidHolder {
          override val RSAPrivateKeyset: Set[KID] = ApplicationConfiguration.rsaKeysIdentifiers
          override val ECPrivateKeyset: Set[KID]  = ApplicationConfiguration.ecKeysIdentifiers
        }
      )
    )

  implicit val contexts: Seq[(String, String)] = Seq(
    CORRELATION_ID_HEADER -> s"interop-be-jobs-${System.currentTimeMillis()}"
  )

  val result: Future[Unit] = for {
    tokenGenerator <- interopTokenGenerator.toFuture
    m2mToken       <- tokenGenerator
      .generateInternalToken(
        subject = jwtConfig.subject,
        audience = jwtConfig.audience.toList,
        tokenIssuer = jwtConfig.issuer,
        secondsDuration = jwtConfig.durationInSeconds
      )
    _ = logger.info("M2M Token obtained")
    _ <- attributeRegistryManagementService.loadCertifiedAttributes(m2mToken.serialized)
  } yield ()

  result.onComplete {
    case Success(_)  =>
      logger.info("Attributes load completed")
      CoordinatedShutdown(classicActorSystem).run(SuccessShutdown)
    case Failure(ex) =>
      logger.error("Attributes load failed", ex)
      CoordinatedShutdown(classicActorSystem).run(ErrorShutdown)
  }

}
