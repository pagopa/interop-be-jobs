package it.pagopa.interop.tenantscertifiedattributesupdater
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.{actor => classic}
import cats.implicits.toTraverseOps
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.attributeregistrymanagement.model.persistence.attribute.PersistentAttribute
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.TypeConversions._
import it.pagopa.interop.tenantmanagement.model.tenant.PersistentTenant
import it.pagopa.interop.tenantprocess.client.model.{ExternalId, InternalTenantSeed}
import it.pagopa.interop.tenantscertifiedattributesupdater.repository.impl.{
  AttributesRepositoryImpl,
  TenantRepositoryImpl
}
import it.pagopa.interop.tenantscertifiedattributesupdater.repository.{AttributesRepository, TenantRepository}
import it.pagopa.interop.tenantscertifiedattributesupdater.service.{PartyRegistryProxyService, TenantProcessService}
import it.pagopa.interop.tenantscertifiedattributesupdater.system.ApplicationConfiguration
import it.pagopa.interop.tenantscertifiedattributesupdater.util.Utils._
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.connection.NettyStreamFactoryFactory
import org.mongodb.scala.{ConnectionString, MongoClient, MongoClientSettings}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

object Main extends App with Dependencies {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  implicit val actorSystem: ActorSystem[Nothing]       =
    ActorSystem[Nothing](Behaviors.empty, "interop-be-tenants-certified-attributes-updater")
  implicit val executionContext: ExecutionContext      = actorSystem.executionContext
  implicit val classicActorSystem: classic.ActorSystem = actorSystem.toClassic

  val blockingEc: ExecutionContextExecutor = actorSystem.dispatchers.lookup(classic.typed.DispatcherSelector.blocking())

  implicit val client: MongoClient = MongoClient(
    MongoClientSettings
      .builder()
      .applyConnectionString(new ConnectionString(ApplicationConfiguration.databaseURL))
      .codecRegistry(DEFAULT_CODEC_REGISTRY)
      .streamFactoryFactory(NettyStreamFactoryFactory())
      .build()
  )

  logger.info("Starting tenants certified attributes updater job")

  val partyRegistryProxyService: PartyRegistryProxyService = partyRegistryProxyService(blockingEc)
  val tenantProcessService: TenantProcessService           = tenantProcessService(blockingEc)

  val tenantRepository: TenantRepository         = TenantRepositoryImpl(client)
  val attributesRepository: AttributesRepository = AttributesRepositoryImpl(client)

  def getAttributesAndTenants: Future[(Seq[PersistentAttribute], Seq[PersistentTenant])] =
    attributesRepository.getAttributes
      .flatMap(_.sequence.toFuture)
      .zip(tenantRepository.getTenants.flatMap(_.sequence.toFuture))
//
//  val result: Future[Unit] = for {
//    bearer                <- generateBearer(jwtConfig, signerService(blockingEc))
//    (attributes, tenants) <- getAttributesAndTenants
//    attributesIndex           = createAttributesIndex(attributes)
//    institutionsRetriever     = partyRegistryProxyService.getInstitutions(bearer)(_, _)
//    tenantUpserter            = tenantProcessService.upsertTenant(bearer)(_)
//    revokerCertifiedAttribute = tenantProcessService.revokeCertifiedAttribute(bearer)(_, _)
//    institutions <- retrieveAllInstitutions(institutionsRetriever, initPage, List.empty)
//    action = createAction(institutions, tenants.toList, attributesIndex)
//    _ <- processActivations(tenantUpserter, action.activations.grouped(groupDimension).toList)
//    _ = logger.info(s"Activated tenants/attributes")
//    _ <- processRevocations(revokerCertifiedAttribute, action.revocations.toList)
//    _ = logger.info(s"Revoked tenants/attributes")
//  } yield ()

  val result: Future[Unit] = for {
    bearer <- generateBearer(jwtConfig, signerService(blockingEc))
    tenants <- tenantRepository.getTenants.flatMap(_.sequence.toFuture)
    total = tenants.count(t => t.kind.isEmpty)
    _ <- tenants
      .filter(t => t.kind.isEmpty)
      .map(t =>
        InternalTenantSeed(
          externalId = ExternalId(origin = t.externalId.origin, value = t.externalId.value),
          certifiedAttributes = Nil,
          name = t.name
        )
      )
      .zipWithIndex
      .traverse { case (t, index) =>
        tenantProcessService.upsertTenant(bearer)(t).map(_ => println(s"$index/$total"))
      }
  } yield ()

  result.onComplete {
    case Failure(ex) =>
      logger.error(s"Failed tenants certified attributes updater job - ${ex.getMessage}")
      shutdown()
    case Success(_)  =>
      logger.info("Completed tenants certified attributes updater job")
      shutdown()
  }

  Await.result(result, Duration.Inf)

}
