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
import it.pagopa.interop.tenantprocess.client.model.{ExternalId, InternalAttributeSeed, InternalTenantSeed}
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
    val fromRegistry: Map[PersistentExternalId, AttributeInfo] =
      institutions
        .filter(_.id.nonEmpty)
        .map { institution =>
          PersistentExternalId(institution.origin, institution.originId) -> AttributeInfo(
            institution.origin,
            institution.category,
            None
          )
        }
        .toMap

    val fromTenant: Map[PersistentExternalId, List[AttributeInfo]] =
      tenants
        .map(tenant =>
          tenant.externalId -> tenant.attributes.flatMap(attr =>
            AttributeInfo.addRevocationTimeStamp(attr, attributesIndex)
          )
        )
        .toMap

    val activations: List[InternalTenantSeed] =
      fromRegistry
        .filterNot { case (extId, attr) => fromTenant.get(extId).exists(AttributeInfo.stillExistsInTenant(attr)) }
        .map { case (extId, attr) =>
          InternalTenantSeed(ExternalId(extId.origin, extId.value), List(InternalAttributeSeed(attr.origin, attr.code)))
        }
        .toList

    val revocations: Map[PersistentExternalId, List[AttributeInfo]] =
      fromTenant.filterNot { case (extId, attrs) =>
        fromRegistry.get(extId).exists(AttributeInfo.stillExistInRegistry(attrs))
      }

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
