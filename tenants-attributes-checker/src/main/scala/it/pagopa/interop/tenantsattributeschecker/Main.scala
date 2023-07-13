package it.pagopa.interop.tenantsattributeschecker

import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.cqrs.service.MongoDbReadModelService
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.queue.config.SQSHandlerConfig
import it.pagopa.interop.commons.queue.impl.SQSHandler
import it.pagopa.interop.commons.utils.service.UUIDSupplier
import it.pagopa.interop.tenantsattributeschecker.ApplicationConfiguration.{
  actorSystem,
  blockingEc,
  certifiedMailQueueName,
  context,
  selfcareV2ApiKey,
  selfcareV2URL
}
import it.pagopa.interop.tenantsattributeschecker.service.QueueService
import it.pagopa.interop.tenantsattributeschecker.service.impl.{TenantProcessServiceImpl, _}
import it.pagopa.interop.tenantsattributeschecker.util.Jobs
import it.pagopa.interop.tenantsattributeschecker.util.ReadModelQueries._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

object Main extends App {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass.getCanonicalName)

  private val readModelService: MongoDbReadModelService = new MongoDbReadModelService(
    ApplicationConfiguration.readModelConfig
  )

  private val queueService: QueueService = {
    val sqsHandlerConfig: SQSHandlerConfig = SQSHandlerConfig(queueUrl = certifiedMailQueueName)
    val sqsHandler: SQSHandler             = SQSHandler(sqsHandlerConfig)(blockingEc)
    new QueueServiceImpl(sqsHandler)
  }

  val jobs = new Jobs(
    agreementProcess = AgreementProcessServiceImpl(blockingEc),
    tenantProcess = TenantProcessServiceImpl(blockingEc),
    attributeRegistryProcess = AttributeRegistryProcessServiceImpl(blockingEc),
    queueService = queueService,
    selfcareClientService = new SelfcareClientServiceImpl(selfcareV2URL, selfcareV2ApiKey),
    uuidSupplier = UUIDSupplier
  )

  logger.info("Starting tenants attributes checker job")

  private val execution: Future[Unit] = for {
    tenantsExpired  <- getAllAttributesTenants(readModelService, getExpiredAttributesTenants)
    _               <- jobs.applyStrategyOnExpiredAttributes(tenantsExpired.toList)
    tenantsExpiring <- getAllAttributesTenants(readModelService, getExpiringAttributesTenants)
    _               <- jobs.applyStrategyOnExpiringAttributes(tenantsExpiring.toList)
  } yield ()

  private val init = execution
    .andThen {
      case Failure(e) => logger.info("Tenants attributes checker job failed with exception", e)
      case Success(_) => logger.info("Completed tenants attributes checker job")
    }
    .andThen { _ =>
      readModelService.close()
      actorSystem.terminate()
    }
    .transformWith(_ => actorSystem.whenTerminated)

  Await.ready(init, Duration.Inf)
}
