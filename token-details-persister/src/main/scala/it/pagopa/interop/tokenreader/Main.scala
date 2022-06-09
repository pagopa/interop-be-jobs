package it.pagopa.interop.tokenreader

import akka.actor.CoordinatedShutdown
import it.pagopa.interop.commons.files.StorageConfiguration
import it.pagopa.interop.commons.files.service.FileManager
import it.pagopa.interop.tokenreader.system.{classicActorSystem, executionContext}
import it.pagopa.interop.tokenreader.utils.FileUtils
import org.slf4j.{Logger, LoggerFactory}

import scala.util.{Failure, Success, Try}

//shuts down the actor system in case of startup errors
case object ErrorShutdown   extends CoordinatedShutdown.Reason
case object SuccessShutdown extends CoordinatedShutdown.Reason

object Main extends App {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  val fileManager: Try[FileManager] = FileManager.getConcreteImplementation(StorageConfiguration.runtimeFileManager)

  fileManager match {
    case Success(manager) =>
      logger.info("File manager instantiated, job can be executed")
      execute(manager)
    case Failure(ex)      =>
      logger.error(s"No valid file manager instance existing for ${StorageConfiguration.runtimeFileManager}", ex)
      CoordinatedShutdown(classicActorSystem).run(ErrorShutdown)
  }

  def execute(fileManager: FileManager): Unit = {
    logger.info("Reading tokens from queue...")
    val jobExecution = JobExecution(new FileUtils(fileManager))
    val result       = jobExecution.run()
    result.onComplete {
      case Success(_)  =>
        logger.info("Copy of tokens on storage bucket completed")
        CoordinatedShutdown(classicActorSystem).run(SuccessShutdown)
      case Failure(ex) =>
        logger.error("Copy of tokens on storage bucket FAILED!", ex)
        CoordinatedShutdown(classicActorSystem).run(ErrorShutdown)
    }
  }

}
