package it.pagopa.interop.attributesloader

import akka.actor.CoordinatedShutdown
import it.pagopa.interop.attributeregistrymanagement.client.api.AttributeApi
import it.pagopa.interop.attributesloader.service.AttributeRegistryManagementInvoker
import it.pagopa.interop.attributesloader.service.impl.AttributeRegistryManagementServiceImpl
import it.pagopa.interop.attributesloader.system.{ApplicationConfiguration, classicActorSystem, executionContext}
import it.pagopa.interop.commons.jwt._
import it.pagopa.interop.commons.jwt.model.RSA
import it.pagopa.interop.commons.jwt.service.InteropTokenGenerator
import it.pagopa.interop.commons.jwt.service.impl.DefaultInteropTokenGenerator
import it.pagopa.interop.commons.utils.TypeConversions.TryOps
import it.pagopa.interop.commons.vault.service.VaultService
import it.pagopa.interop.commons.vault.service.impl.{DefaultVaultClient, DefaultVaultService}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

//shuts down the actor system in case of startup errors
case object ErrorShutdown   extends CoordinatedShutdown.Reason
case object SuccessShutdown extends CoordinatedShutdown.Reason

trait VaultServiceDependency {
  val vaultService: VaultService = new DefaultVaultService with DefaultVaultClient.DefaultClientInstance
}

trait AttributeRegistryManagementDependency {
  val attributeRegistryManagementApi: AttributeApi = AttributeApi(
    ApplicationConfiguration.attributeRegistryManagementURL
  )

  val attributeRegistryManagementService: AttributeRegistryManagementServiceImpl =
    AttributeRegistryManagementServiceImpl(AttributeRegistryManagementInvoker(), attributeRegistryManagementApi)
}

object Main extends App with VaultServiceDependency with AttributeRegistryManagementDependency {

  implicit val logger: Logger = LoggerFactory.getLogger(this.getClass)

  logger.info("Loading attributes data...")

  lazy val jwtConfig: JWTInternalTokenConfig = JWTConfiguration.jwtInternalTokenConfig

  val interopTokenGenerator: Try[InteropTokenGenerator] =
    Try(new DefaultInteropTokenGenerator with PrivateKeysHolder {
      override val RSAPrivateKeyset: Map[KID, SerializedKey] =
        vaultService.readBase64EncodedData(ApplicationConfiguration.rsaPrivatePath)
      override val ECPrivateKeyset: Map[KID, SerializedKey] =
        Map.empty
    })

  val result: Future[Unit] = for {
    tokenGenerator <- interopTokenGenerator.toFuture
    m2mToken <- tokenGenerator
      .generateInternalToken(
        jwtAlgorithmType = RSA,
        subject = jwtConfig.subject,
        audience = jwtConfig.audience.toList,
        tokenIssuer = jwtConfig.issuer,
        secondsDuration = jwtConfig.durationInSeconds
      )
      .toFuture
    _ = logger.info("M2M Token obtained")
    _ <- attributeRegistryManagementService.loadCertifiedAttributes(m2mToken)
  } yield ()

  result.onComplete {
    case Success(_) =>
      logger.info("Attributes load completed")
      CoordinatedShutdown(classicActorSystem).run(SuccessShutdown)
    case Failure(ex) =>
      logger.error("Attributes load failed", ex)
      CoordinatedShutdown(classicActorSystem).run(ErrorShutdown)
  }

}