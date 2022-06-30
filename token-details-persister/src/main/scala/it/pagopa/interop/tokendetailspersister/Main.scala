package it.pagopa.interop.tokendetailspersister

import it.pagopa.interop.commons.files.StorageConfiguration
import it.pagopa.interop.commons.files.service.FileManager
import it.pagopa.interop.tokendetailspersister.FileUtils

import scala.util.{Failure, Success, Try}
import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Await}
import java.util.concurrent.ExecutorService
import scala.concurrent.duration.Duration
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import com.typesafe.config.ConfigFactory

object Main extends App {
  val blockingThreadPool: ExecutorService  = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
  val blockingEC: ExecutionContextExecutor = ExecutionContext.fromExecutor(blockingThreadPool)

  val fileManager: FileManager = FileManager.get(FileManager.S3)(blockingEC)
  val fileUtils: FileUtils     = new FileUtils(fileManager)
  val job: JobExecution        = new JobExecution(fileUtils)(blockingEC)
  val execution: Future[Unit]  = job.run().andThen(_ => blockingThreadPool.shutdown())(global)

  Await.result(execution, Duration.Inf)
}
