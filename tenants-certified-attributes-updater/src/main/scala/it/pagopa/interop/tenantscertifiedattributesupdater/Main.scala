package it.pagopa.interop.tenantscertifiedattributesupdater
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.{actor => classic}
import cats.syntax.all._
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.SprayCommonFormats.uuidFormat
import it.pagopa.interop.partyregistryproxy.client.model.Institution
import it.pagopa.interop.tenantmanagement.client.model.TenantDelta
import it.pagopa.interop.tenantscertifiedattributesupdater.repository.impl.{
  AttributesRepositoryImpl,
  TenantRepositoryImpl
}
import it.pagopa.interop.tenantscertifiedattributesupdater.repository.{AttributesRepository, TenantRepository}
import it.pagopa.interop.tenantscertifiedattributesupdater.service.{PartyRegistryProxyService, TenantManagementService}
import it.pagopa.interop.tenantscertifiedattributesupdater.system.ApplicationConfiguration
import it.pagopa.interop.tenantscertifiedattributesupdater.util._
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.connection.NettyStreamFactoryFactory
import org.mongodb.scala.{ConnectionString, MongoClient, MongoClientSettings}
import spray.json.DefaultJsonProtocol._

import java.util.UUID
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}
import spray.json._

object Main extends App with Dependencies {

  case class CompactExternalId(origin: String, value: String)
  object CompactExternalId {
    implicit val compactExternalIdFormat: RootJsonFormat[CompactExternalId] = jsonFormat2(CompactExternalId.apply)
  }
  case class CompactTenant(id: UUID, externalId: CompactExternalId)
  object CompactTenant     {
    implicit val compactTenantFormat: RootJsonFormat[CompactTenant] = jsonFormat2(CompactTenant.apply)
  }

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  implicit val actorSystem: ActorSystem[Nothing]       =
    ActorSystem[Nothing](Behaviors.empty, "interop-be-tenants-certified-attributes-updater")
  implicit val executionContext: ExecutionContext      = actorSystem.executionContext
  implicit val classicActorSystem: classic.ActorSystem = actorSystem.toClassic

  val blockingEc: ExecutionContextExecutor = actorSystem.dispatchers.lookup(classic.typed.DispatcherSelector.blocking())
//  val tenThreadsEc                         = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(10))

  implicit val client: MongoClient = MongoClient(
    MongoClientSettings
      .builder()
      .applyConnectionString(new ConnectionString(ApplicationConfiguration.databaseURL))
      .codecRegistry(DEFAULT_CODEC_REGISTRY)
      .streamFactoryFactory(NettyStreamFactoryFactory())
      .build()
  )

  logger.info("Starting tenants name update")

  val partyRegistryProxyService: PartyRegistryProxyService = partyRegistryProxyService(blockingEc)
  val tenantManagementService: TenantManagementService     = tenantManagementService(blockingEc)

  val tenantRepository: TenantRepository         = TenantRepositoryImpl(client)
  val attributesRepository: AttributesRepository = AttributesRepositoryImpl(client)

  def addTenantName(tenant: CompactTenant, institutions: Map[String, Institution])(bearer: String): Future[Unit] = {
    val registryInstitution = institutions.get(tenant.externalId.value)
    val tenantDelta         = TenantDelta(name = registryInstitution.map(_.description).getOrElse("Unknown"))

    // TODO Re-enable this for real run
//    logger.info(s"Updating with name ${tenantDelta.name}")
//    val _ = bearer
//    Future.unit

    tenantManagementService.updateTenant(bearer)(tenant.id, tenantDelta)
  }

  def printReport(results: Seq[Either[Throwable, Unit]]): Unit = {
    logger.info(s"Success: ${results.count(_.isRight)} Failures: ${results.count(_.isLeft)}")
    results.collect { case Left(ex) => logger.error(ex.getMessage) }
    ()
  }

  val result: Future[Unit] = for {
    bearer          <- generateBearer(jwtConfig, signerService(blockingEc))
    toUpdateTenants <- tenantRepository.getTenantsWithoutName
    totalCount            = toUpdateTenants.size
    institutionsRetriever = partyRegistryProxyService.getInstitutions(bearer)(_, _)
    institutions <- retrieveAllInstitutions(institutionsRetriever, initPage, List.empty)
    institutionsMap = institutions.map(i => (i.originId, i)).toMap
    _               = logger.info(s"Found $totalCount tenants to update")
    results <- toUpdateTenants.zipWithIndex.traverse {
      case (Left(ex), index) =>
        logger.error(s"Error deserializing element $index from read model", ex)
        Future.successful(Left(ex))
      case (Right(t), index) =>
        logger.info(s"Processing element $index/$totalCount with id ${t.id}")
        addTenantName(t, institutionsMap)(bearer)
          .map(Right(_))
          .recoverWith(ex => Future.successful(Left(ex)))
    }
    _ = printReport(results)
  } yield ()

  result.onComplete {
    case Failure(ex) =>
      logger.error(s"Failed tenants name update - ${ex.getMessage}")
      shutdown()
    case Success(_)  =>
      logger.info("Completed tenants name update")
      shutdown()
  }

  Await.result(result, Duration.Inf)

}
