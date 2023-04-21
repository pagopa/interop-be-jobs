package it.pagopa.interop.eservicesfilegenerator

import it.pagopa.interop.commons.cqrs.service.ReadModelService
import it.pagopa.interop.eservicesfilegenerator.util._
import it.pagopa.interop.commons.logging._
import com.typesafe.scalalogging.LoggerTakingImplicit
import it.pagopa.interop.commons.files.service.FileManager
import io.circe.syntax._
import it.pagopa.interop.eservicesfilegenerator.util.Configuration
import it.pagopa.interop.eservicesfilegenerator.model.EServiceDB
import it.pagopa.interop.eservicesfilegenerator.model.EService
import it.pagopa.interop.eservicesfilegenerator.model.EService._

import scala.concurrent.{Future, ExecutionContext}

object Jobs {

  def getEservices()(implicit ec: ExecutionContext, readModelService: ReadModelService): Future[Seq[EServiceDB]] =
    getAll(50)(ReadModelQueries.getEServices(_, _))(ec)

  private def getAll[T](
    limit: Int
  )(get: (Int, Int) => Future[Seq[T]])(implicit ec: ExecutionContext): Future[Seq[T]] = {
    def go(offset: Int)(acc: Seq[T]): Future[Seq[T]] = {
      get(offset, limit).flatMap(xs =>
        if (xs.size < limit) Future.successful(xs ++ acc)
        else go(offset + xs.size)(xs ++ acc)
      )
    }
    go(0)(Nil)
  }

  def saveIntoBucket(fileManager: FileManager)(eServices: Seq[EService])(implicit
    ec: ExecutionContext,
    configuration: Configuration,
    logger: LoggerTakingImplicit[ContextFieldsToLog],
    context: ContextFieldsToLog
  ): Future[String] =
    for {
      fileName <- Future.successful(s"${configuration.storage.filename}.ndjson")
      lines   = eServices.map(_.asJson.noSpaces)
      _       = logger.info(s"Storing ${lines.size} lines at ${configuration.storage.bucket}/$fileName")
      content = lines.mkString("\n").getBytes()
      result <- fileManager.storeBytes(configuration.storage.bucket, "", fileName)(content)
      _ = logger.info(s"Stored ${lines.size} lines at ${configuration.storage.bucket}/$fileName")

    } yield result

}
