package it.pagopa.interop.privacynoticesupdater.service

import cats.syntax.all._
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.TypeConversions._
import it.pagopa.interop.privacynoticesupdater.util.DynamoConfiguration
import it.pagopa.interop.privacynoticesupdater.model.db.PrivacyNotice
import it.pagopa.interop.privacynoticesupdater.error.PrivacyNoticeError._
import org.scanamo.DynamoReadError.describe
import org.scanamo._
import org.scanamo.syntax._

import scala.concurrent.{Future, ExecutionContext}
import java.util.UUID

trait DynamoService {
  def getById(id: UUID)(implicit contexts: Seq[(String, String)]): Future[Option[PrivacyNotice]]

  def put(privacyNotice: PrivacyNotice)(implicit contexts: Seq[(String, String)]): Future[Unit]

  def delete(id: UUID)(implicit contexts: Seq[(String, String)]): Future[Unit]
}

final class DynamoServiceImpl(config: DynamoConfiguration)(implicit ec: ExecutionContext, scanamo: ScanamoAsync)
    extends DynamoService {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  val table: Table[PrivacyNotice] =
    Table[PrivacyNotice](config.tableName)

  override def put(privacyNotice: PrivacyNotice)(implicit contexts: Seq[(String, String)]): Future[Unit] = {
    logger.info(s"Putting $privacyNotice privacy notice")
    scanamo
      .exec(table.put(privacyNotice))
  }

  override def delete(id: UUID)(implicit contexts: Seq[(String, String)]): Future[Unit] = {
    logger.info(s"Deleting privacy notice with id $id")
    scanamo
      .exec(
        table
          .delete("pk" === s"${PrivacyNotice.pkPrefix}$id" and "sk" === s"${PrivacyNotice.skPrefix}$id")
      )
  }

  override def getById(id: UUID)(implicit contexts: Seq[(String, String)]): Future[Option[PrivacyNotice]] = {
    logger.info(s"Getting id $id privacy notice")
    scanamo
      .exec { table.get("pk" === s"${PrivacyNotice.pkPrefix}$id" and "sk" === s"${PrivacyNotice.skPrefix}$id") }
      .flatMap {
        case Some(value) => value.leftMap(err => DynamoReadingError(describe(err))).toFuture.some.sequence
        case None        => Future.successful(None)
      }
  }
}