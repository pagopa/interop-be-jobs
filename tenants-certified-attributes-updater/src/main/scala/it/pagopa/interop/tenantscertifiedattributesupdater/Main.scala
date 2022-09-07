package it.pagopa.interop.tenantscertifiedattributesupdater
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.{actor => classic}
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.tenantscertifiedattributesupdater.repository.impl.{
  AttributesRepositoryImpl,
  TenantRepositoryImpl
}
import it.pagopa.interop.tenantscertifiedattributesupdater.repository.{AttributesRepository, TenantRepository}
import it.pagopa.interop.tenantscertifiedattributesupdater.service.{PartyRegistryProxyService, TenantProcessService}
import it.pagopa.interop.tenantscertifiedattributesupdater.system.ApplicationConfiguration
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
      .applyConnectionString(new ConnectionString(ApplicationConfiguration.tenantsDatabaseUrl))
      .codecRegistry(DEFAULT_CODEC_REGISTRY)
      .streamFactoryFactory(NettyStreamFactoryFactory())
      .build()
  )

  logger.info("Starting tenants certified attributes updater job")

  val partyRegistryProxyService: PartyRegistryProxyService = partyRegistryProxyService(blockingEc)
  val tenantProcessService: TenantProcessService           = tenantProcessService(blockingEc)

  val tenantRepository: TenantRepository         = TenantRepositoryImpl(client)
  val attributesRepository: AttributesRepository = AttributesRepositoryImpl(client)

  val result: Future[Unit] = for {
    bearer     <- generateBearer(blockingEc)
    attributes <- attributesRepository.getAttributes
    tenants    <- tenantRepository.getTenants
    attributesIndex       = createAttributesIndex(attributes)
    institutionsRetriever = partyRegistryProxyService.getInstitutions(bearer)(_, _)
    tenantService         = tenantProcessService.upsertTenant(bearer)(_)
    institutions <- retrieveAllInstitutions(institutionsRetriever, initPage, List.empty)
    action = createAction(institutions, tenants, attributesIndex)
    _ <- processActivations(tenantService, action.activations.grouped(groupDimension).toList)
    _ = logger.info(s"Activated tenants/attributes")
  } yield ()

  result.onComplete {
    case Failure(ex) =>
      logger.error(s"Failed tenants certified attributes updater job - ${ex.getMessage}")
      client.close()
      actorSystem.terminate()
    case Success(_)  =>
      logger.info("Completed tenants certified attributes updater job")
      client.close()
      actorSystem.terminate()
  }

  Await.result(result, Duration.Inf)

}
