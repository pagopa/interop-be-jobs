package it.pagopa.interop.tenantscertifiedattributesupdater

import akka.actor.typed.ActorSystem
import cats.implicits._
import com.typesafe.scalalogging.LoggerTakingImplicit
import it.pagopa.interop.attributeregistrymanagement.model.persistence.attribute.{Certified, PersistentAttribute}
import it.pagopa.interop.commons.jwt.service.InteropTokenGenerator
import it.pagopa.interop.commons.jwt.service.impl.DefaultInteropTokenGenerator
import it.pagopa.interop.commons.jwt.{JWTInternalTokenConfig, KID, PrivateKeysKidHolder}
import it.pagopa.interop.commons.logging.ContextFieldsToLog
import it.pagopa.interop.commons.signer.service.SignerService
import it.pagopa.interop.partyregistryproxy.client.model.{Institution, Institutions}
import it.pagopa.interop.tenantmanagement.model.tenant.{PersistentExternalId, PersistentTenant}
import it.pagopa.interop.tenantprocess.client.model.InternalTenantSeed
import it.pagopa.interop.tenantscertifiedattributesupdater.system.ApplicationConfiguration
import org.mongodb.scala.MongoClient

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

package object util {
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
    val fromRegistry: List[TenantSeed] =
      institutions
        .filter(_.id.nonEmpty)
        .map(institution =>
          TenantSeed(
            TenantId(institution.origin, institution.originId, institution.description),
            AttributeInfo(institution.origin, institution.category, None)
          )
        )

    val fromTenant: Map[PersistentExternalId, List[AttributeInfo]] =
      tenants
        .map(tenant =>
          tenant.externalId -> tenant.attributes.flatMap(attr =>
            AttributeInfo.addRevocationTimeStamp(attr, attributesIndex)
          )
        )
        .toMap

    val tenantsMap: Map[PersistentExternalId, String] = tenants.map(t => (t.externalId, t.name)).toMap
    val activations: List[InternalTenantSeed]         =
      fromRegistry
        .filterNot(tenantSeed =>
          fromTenant
            .get(tenantSeed.id.toPersistentExternalId)
            .exists(AttributeInfo.stillExistsInTenant(tenantSeed.attributeInfo))
        )
        .groupMapReduce[TenantId, List[AttributeInfo]](_.id)(tenantSeed => List(tenantSeed.attributeInfo))(_ ++ _)
        .toList
        .map { case (tenantId, attrs) =>
          InternalTenantSeed(
            tenantId.toExternalId,
            attrs.map(_.toInternalAttributeSeed),
            tenantsMap.getOrElse(tenantId.toPersistentExternalId, tenantId.name)
          )
        }

    val revocations: Map[PersistentExternalId, List[AttributeInfo]] =
      fromTenant.toList
        .flatMap { case (externalId, attrs) => List(externalId).zip(attrs) }
        .filterNot { case (tenantId, attributeFromTenant) =>
          fromRegistry.exists { tenantSeed =>
            tenantSeed.id.toPersistentExternalId == tenantId &&
            attributeFromTenant.code == tenantSeed.attributeInfo.code &&
            attributeFromTenant.origin == tenantSeed.attributeInfo.origin
          }
        }
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

  def shutdown()(implicit client: MongoClient, actorSystem: ActorSystem[_]): Unit = {
    client.close()
    actorSystem.terminate()
  }
}
