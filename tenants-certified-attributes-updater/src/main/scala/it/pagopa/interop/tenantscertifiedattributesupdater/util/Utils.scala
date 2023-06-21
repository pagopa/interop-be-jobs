package it.pagopa.interop.tenantscertifiedattributesupdater.util

import akka.actor.typed.ActorSystem
import cats.implicits._
import com.typesafe.scalalogging.LoggerTakingImplicit
import it.pagopa.interop.attributeregistrymanagement.model.persistence.attribute.{Certified, PersistentAttribute}
import it.pagopa.interop.commons.jwt.service.InteropTokenGenerator
import it.pagopa.interop.commons.jwt.service.impl.DefaultInteropTokenGenerator
import it.pagopa.interop.commons.jwt.{JWTInternalTokenConfig, KID, PrivateKeysKidHolder}
import it.pagopa.interop.commons.logging.ContextFieldsToLog
import it.pagopa.interop.commons.signer.service.SignerService
import it.pagopa.interop.commons.utils.Digester
import it.pagopa.interop.commons.utils.TypeConversions._
import it.pagopa.interop.partyregistryproxy.client.model.{Institution, Institutions}
import it.pagopa.interop.tenantmanagement.model.tenant.{PersistentExternalId, PersistentTenant}
import it.pagopa.interop.attributeregistryprocess.Utils.kindToBeExcluded
import it.pagopa.interop.tenantprocess.client.model.{ExternalId, InternalAttributeSeed, InternalTenantSeed}
import it.pagopa.interop.tenantscertifiedattributesupdater.system.ApplicationConfiguration
import org.mongodb.scala.MongoClient

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

object Utils {

  implicit class AttributeInfoOps(val a: AttributeInfo) extends AnyVal {
    def toInternalAttributeSeed: InternalAttributeSeed = InternalAttributeSeed(a.origin, a.code)
  }

  implicit class TenantIdOps(val t: TenantId) extends AnyVal {
    def toPersistentExternalId: PersistentExternalId = PersistentExternalId(t.origin, t.value)

    def toExternalId: ExternalId = ExternalId(t.origin, t.value)
  }

  def generateBearer(jwtConfig: JWTInternalTokenConfig, signerService: SignerService)(implicit
    ec: ExecutionContext
  ): Future[String] = for {
    tokenGenerator <- interopTokenGenerator(signerService)
    m2mToken       <- tokenGenerator
      .generateInternalToken(
        subject = jwtConfig.subject,
        audience = jwtConfig.audience.toList,
        tokenIssuer = jwtConfig.issuer,
        secondsDuration = jwtConfig.durationInSeconds
      )
  } yield m2mToken.serialized

  private def interopTokenGenerator(
    signerService: SignerService
  )(implicit ec: ExecutionContext): Future[InteropTokenGenerator] = Future(
    new DefaultInteropTokenGenerator(
      signerService,
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

    val originsFromPartyRegistry: List[String] = institutions.map(_.origin).distinct

    val filteredAttributesIndex: Map[UUID, AttributeInfo] =
      attributesIndex.filter(a => originsFromPartyRegistry.contains(a._2.origin))

    val fromRegistry: List[TenantSeed] = institutions.filter(_.id.nonEmpty).map(createTenantSeed)

    val fromTenant: Map[PersistentExternalId, List[AttributeInfo]] =
      tenants
        .map(tenant =>
          tenant.externalId -> tenant.attributes.flatMap(AttributeInfo.addRevocationTimeStamp(filteredAttributesIndex))
        )
        .toMap

    val tenantsMap: Map[PersistentExternalId, String] = tenants.map(t => (t.externalId, t.name)).toMap

    val activations: List[InternalTenantSeed] =
      fromRegistry
        .map(extractActivable(fromTenant))
        .filter(_.attributesInfo.nonEmpty)
        .groupMapReduce[TenantId, Set[AttributeInfo]](_.id)(tenantSeed => tenantSeed.attributesInfo.toSet)(_ ++ _)
        .toList
        .map { case (tenantId, attrs) =>
          InternalTenantSeed(
            tenantId.toExternalId,
            attrs.map(_.toInternalAttributeSeed).toList,
            tenantsMap.getOrElse(tenantId.toPersistentExternalId, tenantId.name)
          )
        }

    val revocations: Map[PersistentExternalId, List[AttributeInfo]] =
      fromTenant.toList
        .flatMap { case (externalId, attrs) => attrs.tupleLeft(externalId) }
        .flatMap(extractRevocable(fromRegistry).tupled)
        .groupMap[PersistentExternalId, AttributeInfo](_._1)(_._2)

    TenantActions(activations, revocations)

  }

  def createAttributesIndex(attributes: Seq[PersistentAttribute]): Map[UUID, AttributeInfo] =
    attributes
      .filter(_.kind == Certified)
      .mapFilter(attribute =>
        attribute.origin.zip(attribute.code).map { case (origin, code) =>
          attribute.id -> AttributeInfo(origin, code, None)
        }
      )
      .toMap

  final val initPage: Int       = 1
  final val maxLimit: Int       = 100
  final val groupDimension: Int = 1000

  def retrieveAllInstitutions(
    institutionsRetriever: (Int, Int) => Future[Institutions],
    page: Int,
    acc: List[Institution]
  )(implicit
    actorSystem: ActorSystem[_],
    executionContext: ExecutionContext,
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog]
  ): Future[List[Institution]] = institutionsRetriever(page, maxLimit).flatMap(institutions =>
    if (institutions.items.isEmpty) {
      logger.info(s"Load completed size ${acc.size}")
      Future.successful(acc)
    } else retrieveAllInstitutions(institutionsRetriever, page + 1, acc ++ institutions.items)
  )

  def processActivations(tenantUpserter: InternalTenantSeed => Future[Unit], list: List[List[InternalTenantSeed]])(
    implicit
    actorSystem: ActorSystem[_],
    contexts: Seq[(String, String)],
    executionContext: ExecutionContext,
    logger: LoggerTakingImplicit[ContextFieldsToLog]
  ): Future[Unit] = list match {
    case Nil       => Future.unit
    case x :: Nil  => Future.traverse(x)(tenantUpserter).void
    case xs :: xss =>
      logger.info(s"Processing group ${list.size}")
      Future.traverse(xs)(tenantUpserter).flatMap { _ =>
        logger.info(s"Processing new tenants group (still ${xss.size} groups to process)")
        processActivations(tenantUpserter, xss)
      }
  }

  def processRevocations(
    revoker: (PersistentExternalId, AttributeInfo) => Future[Unit],
    list: List[(PersistentExternalId, List[AttributeInfo])]
  )(implicit
    contexts: Seq[(String, String)],
    executionContext: ExecutionContext,
    logger: LoggerTakingImplicit[ContextFieldsToLog]
  ): Future[Unit] =
    Future
      .traverseWithLatch(10)(list) { case (externalId, attributeInfos) =>
        logger.info(s"Processing tenant $externalId with attributes ${attributeInfos.mkString(",")}")
        Future.traverseWithLatch(10)(attributeInfos)(revoker(externalId, _))
      }
      .map(_ => ())

  private def createTenantSeed(institution: Institution): TenantSeed = {
    val attributesWithoutKind: List[AttributeInfo] = List(
      AttributeInfo(institution.origin, institution.category, None),
      AttributeInfo(institution.origin, institution.originId, None)
    )

    // Including only Pubbliche Amministrazioni kind
    val shouldKindBeExcluded: Boolean = kindToBeExcluded.contains(institution.kind)

    val attributes: List[AttributeInfo] =
      if (shouldKindBeExcluded) attributesWithoutKind
      else
        AttributeInfo(institution.origin, Digester.toSha256(institution.kind.getBytes), None) :: attributesWithoutKind

    TenantSeed(TenantId(institution.origin, institution.originId, institution.description), attributes)
  }

  private def extractActivable(fromTenant: Map[PersistentExternalId, List[AttributeInfo]]): TenantSeed => TenantSeed =
    tenantSeed =>
      tenantSeed.copy(attributesInfo = tenantSeed.attributesInfo.filter(canBeActivated(fromTenant, tenantSeed.id)))

  private def canBeActivated(
    fromTenant: Map[PersistentExternalId, List[AttributeInfo]],
    id: TenantId
  ): AttributeInfo => Boolean = attributeFromRegistry => !canNotBeActivated(fromTenant, id, attributeFromRegistry)

  private def canNotBeActivated(
    fromTenant: Map[PersistentExternalId, List[AttributeInfo]],
    id: TenantId,
    attributeFromRegistry: AttributeInfo
  ): Boolean = fromTenant
    .get(id.toPersistentExternalId)
    .exists(attributesFromTenant => // check if exist in tenant
      attributesFromTenant.exists { attributeFromTenant =>
        attributeFromTenant.code == attributeFromRegistry.code &&
        attributeFromTenant.origin == attributeFromRegistry.origin &&
        attributeFromTenant.revocationTimestamp.isEmpty
      }
    )

  private def extractRevocable(
    fromRegistry: List[TenantSeed]
  ): (PersistentExternalId, AttributeInfo) => Option[(PersistentExternalId, AttributeInfo)] =
    (tenantId, attributeFromTenant) =>
      Option.when(canBeRevoked(fromRegistry, tenantId, attributeFromTenant))((tenantId, attributeFromTenant))

  private def canBeRevoked(
    fromRegistry: List[TenantSeed],
    tenantId: PersistentExternalId,
    attributeFromTenant: AttributeInfo
  ): Boolean = !canNotBeRevoked(fromRegistry, tenantId, attributeFromTenant)

  private def canNotBeRevoked(
    fromRegistry: List[TenantSeed],
    tenantId: PersistentExternalId,
    attributeFromTenant: AttributeInfo
  ): Boolean = fromRegistry.exists { tenantSeed => // check if exists in registry
    tenantSeed.id.toPersistentExternalId == tenantId &&
    tenantSeed.attributesInfo
      .exists(attributeFromRegistry =>
        attributeFromTenant.code == attributeFromRegistry.code &&
          attributeFromTenant.origin == attributeFromRegistry.origin
      )
  }

  def shutdown()(implicit client: MongoClient, actorSystem: ActorSystem[_]): Unit = {
    client.close()
    actorSystem.terminate()
  }
}
