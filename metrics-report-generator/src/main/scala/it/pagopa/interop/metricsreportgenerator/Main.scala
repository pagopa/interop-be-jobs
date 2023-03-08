package it.pagopa.interop.metricsreportgenerator

import com.typesafe.scalalogging.Logger
// import it.pagopa.interop.commons.cqrs.service.{MongoDbReadModelService, ReadModelService}
import it.pagopa.interop.commons.files.service.FileManager
// import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier
// import it.pagopa.interop.metricsreportgenerator.report.AgreementRecord
import it.pagopa.interop.metricsreportgenerator.util._

import java.util.concurrent.{ExecutorService, Executors}
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}
import java.util.UUID
import it.pagopa.interop.commons.logging._
import com.typesafe.scalalogging.LoggerTakingImplicit
import it.pagopa.interop.commons.utils.CORRELATION_ID_HEADER
// import it.pagopa.interop.commons.cqrs.model.ReadModelConfig
import it.pagopa.interop.commons.cqrs.service.{ReadModelService, MongoDbReadModelService}
import scala.util.Failure

object Main extends App {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass.getCanonicalName())

  implicit val context: List[(String, String)] = (CORRELATION_ID_HEADER -> UUID.randomUUID().toString()) :: Nil

  logger.info("Starting metrics report generator job")

  def ecs(): Future[(ExecutorService, ExecutionContextExecutor)] = Future {
    val blockingThreadPool: ExecutorService =
      Executors.newFixedThreadPool(1.max(Runtime.getRuntime.availableProcessors() - 1))
    (blockingThreadPool, ExecutionContext.fromExecutor(blockingThreadPool))
  }(global)

  def createFileManager(blockingEC: ExecutionContextExecutor): Future[FileManager] =
    Future(FileManager.get(FileManager.S3)(blockingEC))(global)

  def createReadModel(configuration: Configuration): Future[ReadModelService] =
    Future(new MongoDbReadModelService(configuration.readModel))(global)

  def createJobsContainer(config: Configuration, fm: FileManager, rm: ReadModelService): Future[Jobs] =
    Future(new Jobs(config, fm, rm))(global)

  def resources(): Future[(ExecutorService, ExecutionContextExecutor, FileManager, ReadModelService, Jobs)] = {
    implicit val ec: ExecutionContext = global
    for {
      config           <- Configuration.read()
      (es, blockingEC) <- ecs()
      fm               <- createFileManager(blockingEC)
      rm               <- createReadModel(config)
      jobs             <- createJobsContainer(config, fm, rm)
    } yield (es, blockingEC, fm, rm, jobs)
  }

  def execution(jobs: Jobs)(implicit blockingEC: ExecutionContextExecutor): Future[Unit] = {
    val job1 = jobs.getAgreementRecord.map(xs => println(xs.mkString("\n")))
    val job2 = jobs.getDescriptorsRecord.map { case (xs, ys) =>
      println(xs.mkString("\n"))
      println(ys.mkString("\n"))
    }
    val job3 = jobs.getTokensData.map(xs => println(xs.mkString("\n")))
    job1.zip(job2).zip(job3).map(_ => ())
  }

  def job(): Future[Unit] = resources().flatMap { case (es, blockingEC, fm, rm, jobs) =>
    execution(jobs)(blockingEC)
      .andThen { case Failure(ex) => logger.error("Metrics job got an error", ex) }(global)
      .andThen { _ =>
        es.shutdown()
        fm.close()
        rm.close()
      }(global)
  }(global)

  Await.result(job(), Duration.Inf)
  logger.info("Completed metrics report generator job")
}
