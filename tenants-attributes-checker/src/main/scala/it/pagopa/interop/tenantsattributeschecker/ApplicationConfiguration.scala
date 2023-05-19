package it.pagopa.interop.tenantsattributeschecker

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.{actor => classic}
import com.typesafe.config.{Config, ConfigFactory}
import it.pagopa.interop.commons.cqrs.model.ReadModelConfig
import it.pagopa.interop.commons.utils.CORRELATION_ID_HEADER
import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier

import java.util.UUID
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

object ApplicationConfiguration {

  implicit val actorSystem: ActorSystem[Nothing]       =
    ActorSystem[Nothing](Behaviors.empty, "interop-be-tenants-attributes-checker")
  implicit val blockingEc: ExecutionContextExecutor    =
    actorSystem.dispatchers.lookup(classic.typed.DispatcherSelector.blocking())
  implicit val executionContext: ExecutionContext      = actorSystem.executionContext
  implicit val classicActorSystem: classic.ActorSystem = actorSystem.toClassic
  implicit val context: List[(String, String)]         = (CORRELATION_ID_HEADER -> UUID.randomUUID().toString) :: Nil

  val config: Config                           = ConfigFactory.load()
  val dateTimeSupplier: OffsetDateTimeSupplier = OffsetDateTimeSupplier

  val tenantProcessURL: String    =
    config.getString("services.tenant-process")
  val agreementProcessURL: String =
    config.getString("services.agreement-process")
  val tenantManagementURL: String = config.getString("services.tenant-management")

  val tenantsCollection: String =
    config.getString("read-model.collections.tenants")

  val readModelConfig: ReadModelConfig = {
    val connectionString: String = config.getString("read-model.connection-string")
    val dbName: String           = config.getString("read-model.name")

    ReadModelConfig(connectionString, dbName)
  }
}
