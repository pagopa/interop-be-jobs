package it.pagopa.interop.eservicesmonitoringexporter.util

import it.pagopa.interop.commons.cqrs.service.ReadModelService
import it.pagopa.interop.eservicesmonitoringexporter.util._
import it.pagopa.interop.commons.logging._
import com.typesafe.scalalogging.LoggerTakingImplicit
import it.pagopa.interop.commons.files.service.FileManager
import io.circe.syntax._
import it.pagopa.interop.eservicesmonitoringexporter.util.Configuration
import it.pagopa.interop.eservicesmonitoringexporter.model.EServiceDB
import it.pagopa.interop.eservicesmonitoringexporter.model.EService
import it.pagopa.interop.eservicesmonitoringexporter.model.EService._

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
    configuration: Configuration,
    logger: LoggerTakingImplicit[ContextFieldsToLog],
    context: ContextFieldsToLog
  ): Future[String] = {

    def saveAsNdjson: (String, Array[Byte]) = {
      logger.info(s"Using ndjson")
      val fileName = s"${configuration.storage.filename}.ndjson"
      val lines    = eServices.map(_.asJson.noSpaces)
      val content  = lines.mkString("\n").getBytes()
      (fileName, content)
    }

    def saveAsJson: (String, Array[Byte]) = {
      logger.info(s"Using json")
      val fileName = s"${configuration.storage.filename}.json"
      val content  = eServices.asJson.noSpaces.getBytes()
      (fileName, content)
    }

    val (fileName, content): (String, Array[Byte]) = if (configuration.storage.asndjson) saveAsNdjson else saveAsJson
    fileManager.storeBytes(configuration.storage.bucket, "", fileName)(content)
  }
}
