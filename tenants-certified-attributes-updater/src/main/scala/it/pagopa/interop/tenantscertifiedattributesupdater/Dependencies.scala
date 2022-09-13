package it.pagopa.interop.tenantscertifiedattributesupdater

import akka.actor.typed.ActorSystem
import cats.implicits._
import com.typesafe.scalalogging.LoggerTakingImplicit
import it.pagopa.interop.attributeregistrymanagement.model.persistence.attribute.{Certified, PersistentAttribute}
import it.pagopa.interop.commons.jwt.service.InteropTokenGenerator
import it.pagopa.interop.commons.jwt.service.impl.DefaultInteropTokenGenerator
import it.pagopa.interop.commons.jwt.{JWTConfiguration, JWTInternalTokenConfig, KID, PrivateKeysKidHolder}
import it.pagopa.interop.commons.logging.ContextFieldsToLog
import it.pagopa.interop.commons.signer.service.SignerService
import it.pagopa.interop.commons.signer.service.impl.KMSSignerService
import it.pagopa.interop.commons.utils.TypeConversions.TryOps
import it.pagopa.interop.partyregistryproxy.client.api.InstitutionApi
import it.pagopa.interop.partyregistryproxy.client.model.{Institution, Institutions}
import it.pagopa.interop.tenantmanagement.model.tenant.{PersistentExternalId, PersistentTenant}
import it.pagopa.interop.tenantprocess.client.api.TenantApi
import it.pagopa.interop.tenantprocess.client.model.{ExternalId, InternalAttributeSeed, InternalTenantSeed}
import it.pagopa.interop.tenantscertifiedattributesupdater.service.impl.{
  PartyRegistryProxyServiceImpl,
  TenantProcessServiceImpl
}
import it.pagopa.interop.tenantscertifiedattributesupdater.service.{PartyRegistryProxyInvoker, TenantProcessInvoker}
import it.pagopa.interop.tenantscertifiedattributesupdater.system.ApplicationConfiguration
import it.pagopa.interop.tenantscertifiedattributesupdater.util.{AttributeInfo, TenantActions}
import org.mongodb.scala.MongoClient

import java.util.UUID
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.Try

trait Dependencies {

  implicit val contexts: Seq[(String, String)] = Seq.empty

  final val initPage: Int       = 1
  final val maxLimit: Int       = 100
  final val groupDimension: Int = 1000

  val jwtConfig: JWTInternalTokenConfig = JWTConfiguration.jwtInternalTokenConfig

  def partyRegistryProxyService(
    blockingEc: ExecutionContextExecutor
  )(implicit actorSystem: ActorSystem[_]): PartyRegistryProxyServiceImpl =
    PartyRegistryProxyServiceImpl(
      PartyRegistryProxyInvoker(blockingEc)(actorSystem.classicSystem),
      InstitutionApi(ApplicationConfiguration.partyRegistryProxyURL)
    )(blockingEc)

  def tenantProcessService(
    blockingEc: ExecutionContextExecutor
  )(implicit actorSystem: ActorSystem[_]): TenantProcessServiceImpl =
    TenantProcessServiceImpl(
      TenantProcessInvoker(blockingEc)(actorSystem.classicSystem),
      TenantApi(ApplicationConfiguration.tenantProcessURL)
    )(blockingEc)

  def signerService(blockingEc: ExecutionContextExecutor): SignerService = new KMSSignerService(blockingEc)

  def generateBearer(
    blockingEc: ExecutionContextExecutor
  )(implicit actorSystem: ActorSystem[_], ec: ExecutionContext): Future[String] = for {
    tokenGenerator <- interopTokenGenerator(blockingEc)
    m2mToken       <- tokenGenerator
      .generateInternalToken(
        subject = jwtConfig.subject,
        audience = jwtConfig.audience.toList,
        tokenIssuer = jwtConfig.issuer,
        secondsDuration = jwtConfig.durationInSeconds
      )
  } yield m2mToken.serialized

  def interopTokenGenerator(
    blockingEc: ExecutionContextExecutor
  )(implicit actorSystem: ActorSystem[_], ec: ExecutionContext): Future[InteropTokenGenerator] =
    Future(
      new DefaultInteropTokenGenerator(
        signerService(blockingEc),
        new PrivateKeysKidHolder {
          override val RSAPrivateKeyset: Set[KID] = ApplicationConfiguration.rsaKeysIdentifiers
          override val ECPrivateKeyset: Set[KID]  = ApplicationConfiguration.ecKeysIdentifiers
        }
      )
    )

  def createAction(
    institutions: List[Institution],
    tenants: List[PersistentTenant],
    attributesIndex: Map[UUID, AttributeInfo]
  ): TenantActions = {
    val fromRegistry: Map[PersistentExternalId, AttributeInfo] =
      institutions
        .filter(_.id.nonEmpty)
        .map { institution =>
          PersistentExternalId(institution.origin, institution.id) -> AttributeInfo(
            institution.origin,
            institution.category
          )
        }
        .toMap

    val fromTenant: Map[PersistentExternalId, List[AttributeInfo]] =
      tenants.map(tenant => tenant.externalId -> tenant.attributes.flatMap(attr => attributesIndex.get(attr.id))).toMap

    val activations: List[InternalTenantSeed] =
      fromRegistry
        .filterNot(i => fromTenant.contains(i._1))
        .map { case (extId, attr) =>
          InternalTenantSeed(ExternalId(extId.origin, extId.value), Seq(InternalAttributeSeed(attr.origin, attr.code)))
        }
        .toList

    val revocations: Map[PersistentExternalId, List[AttributeInfo]] =
      fromTenant.filter { case (extId, _) => !fromRegistry.contains(extId) }

    TenantActions(activations, revocations)
  }

  def createAttributesIndex(attributes: Seq[PersistentAttribute]): Map[UUID, AttributeInfo] =
    attributes
      .filter(_.kind == Certified)
      .mapFilter(attribute =>
        attribute.origin.zip(attribute.code).map { case (origin, code) => attribute.id -> AttributeInfo(origin, code) }
      )
      .toMap

  def retrieveAllInstitutions(
    institutionsRetriever: (Int, Int) => Future[Institutions],
    page: Int,
    acc: List[Institution]
  )(implicit
    actorSystem: ActorSystem[_],
    executionContext: ExecutionContext,
    logger: LoggerTakingImplicit[ContextFieldsToLog]
  ): Future[List[Institution]] = institutionsRetriever(page, maxLimit).flatMap(institutions =>
    if (institutions.totalCount == acc.size) {
      logger.info(s"Load completed size ${acc.size}")
      Future.successful(acc)
    } else retrieveAllInstitutions(institutionsRetriever, page + 1, acc ++ institutions.items)
  )

  def processActivations(tenantService: InternalTenantSeed => Future[Unit], list: List[List[InternalTenantSeed]])(
    implicit
    actorSystem: ActorSystem[_],
    executionContext: ExecutionContext,
    logger: LoggerTakingImplicit[ContextFieldsToLog]
  ): Future[Unit] = list match {
    case Nil       => Future.unit
    case x :: Nil  => Future.traverse(x)(tenantService).void
    case xs :: xss =>
      logger.info(s"Processing group ${list.size}")
      Future.traverse(xs)(tenantService).flatMap { _ =>
        logger.info(s"Processing new tenants group (still ${xss.size} groups to process)")
        processActivations(tenantService, xss)
      }
  }

  def shutdown()(implicit client: MongoClient, actorSystem: ActorSystem[_]): Unit = {
    client.close()
    actorSystem.terminate()
  }

}
