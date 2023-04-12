package it.pagopa.interop.dashboardmetricsreportgenerator

import com.typesafe.scalalogging.Logger
import it.pagopa.interop.commons.cqrs.service.{MongoDbReadModelService, ReadModelService}
import it.pagopa.interop.commons.files.service.FileManager
import it.pagopa.interop.dashboardmetricsreportgenerator.Configuration
import it.pagopa.interop.commons.cqrs.model.ReadModelConfig
import spray.json._
import java.util.concurrent.{ExecutorService, Executors}
import scala.concurrent.ExecutionContext.global
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util._
import it.pagopa.interop.commons.logging._
import com.typesafe.scalalogging.LoggerTakingImplicit
import java.util.UUID
import it.pagopa.interop.commons.utils.CORRELATION_ID_HEADER

object Main extends App {

  val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass.getCanonicalName())

  implicit val context: List[(String, String)] = (CORRELATION_ID_HEADER -> UUID.randomUUID().toString()) :: Nil

  logger.info("Starting dashboard metrics report generator job")

  def getFileManager(): Future[(FileManager, ExecutorService)] = Future {
    val blockingThreadPool: ExecutorService = Executors.newFixedThreadPool(Runtime.getRuntime.availableProcessors())
    (FileManager.get(FileManager.S3)(ExecutionContext.fromExecutor(blockingThreadPool)), blockingThreadPool)
  }(global)

  def getReadModel(readModelConfig: ReadModelConfig): Future[ReadModelService] =
    Future(new MongoDbReadModelService(readModelConfig))(global)

  def getPartyManager(partyManagerConfig: PartyManagementConfiguration): Future[PartyManagementProxy] =
    Future(new PartyManagementProxy(partyManagerConfig))(global)

  def resources()(implicit
    global: ExecutionContext
  ): Future[(Configuration, FileManager, ReadModelService, PartyManagementProxy, ExecutorService)] = for {
    config   <- Configuration.read()
    (fm, es) <- getFileManager()
    rm       <- getReadModel(config.readModel)
    pm       <- getPartyManager(config.partyManagement)
  } yield (config, fm, rm, pm, es)

  def saveIntoBucket(fm: FileManager, config: StorageBucketConfiguration)(
    data: DashboardData
  )(implicit global: ExecutionContext): Future[Unit] = {
    logger.info(s"Saving dashboard information at ${config.bucket}/${config.filename}")
    fm.storeBytes(config.bucket, "", config.filename)(data.toJson.compactPrint.getBytes()).map(_ => ())
  }

  def job(config: Configuration, fm: FileManager, rm: ReadModelService, pm: PartyManagementProxy)(implicit
    global: ExecutionContext
  ): Future[Unit] = {
    // * These are val on purpose, to let them start in parallel
    val descriptorsF: Future[DescriptorsData] = Jobs.getDescriptorData(rm, config.collections)
    val tenantsF: Future[TenantsData]         = Jobs.getTenantsData(rm, pm, config.collections, config.overrides)
    val agreementsF: Future[AgreementsData]   = Jobs.getAgreementsData(rm, config.collections)
    val purposesF: Future[PurposesData]       = Jobs.getPurposesData(rm, config.collections)
    val tokensF: Future[TokensData]           = Jobs.getTokensData(fm, config.tokensStorage)

    for {
      descriptors <- descriptorsF
      tenants     <- tenantsF
      agreements  <- agreementsF
      purposes    <- purposesF
      tokens      <- tokensF
      data = DashboardData(descriptors, tenants, agreements, purposes, tokens)
      _ <- saveIntoBucket(fm, config.storage)(data)
    } yield ()
  }

  def app()(implicit global: ExecutionContext): Future[Unit] = resources()
    .flatMap { case (config, fm, rm, pm, ec) =>
      job(config, fm, rm, pm)
        .andThen { case Failure(e) =>
          logger.error("Dashboard metrics report generator job got an error", e)
        }
        .transformWith(_ => pm.close())
        .andThen { _ =>
          fm.close()
          rm.close()
          ec.shutdown()
        }
    }

  Await.ready(app()(global), Duration.Inf): Unit
  logger.info("Completed dashboard metrics report generator job")
}
