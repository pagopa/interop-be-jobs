package it.pagopa.interop.metricsreportgenerator.util

import cats.Functor
import cats.syntax.all._
import com.typesafe.scalalogging.LoggerTakingImplicit
import it.pagopa.interop.commons.cqrs.service.ReadModelService
import it.pagopa.interop.commons.logging._
import it.pagopa.interop.metricsreportgenerator.util.models.MetricDescriptor

import scala.concurrent.{ExecutionContext, Future}

class Jobs(config: Configuration, readModel: ReadModelService)(implicit
  logger: LoggerTakingImplicit[ContextFieldsToLog],
  context: ContextFieldsToLog
) {

  def getAgreementRecord(implicit ec: ExecutionContext): Future[String] = {
    logger.info("Gathering Agreements Information")

    ReadModelQueries
      .getAllActiveAgreements(config.collections, readModel)
      .zip(ReadModelQueries.getAllPurposes(config.collections, readModel))
      .map { case (agreements, purposes) =>
        val header = "eserviceId,eservice,producer,consumer,agreementId,purposes,purposeIds"
        (header :: agreements.toList.map { a =>
          val (purposeIds, purposeNames): (Seq[String], Seq[String]) = purposes
            .filter(p => p.consumerId == a.consumerId && p.eserviceId == a.eserviceId)
            .map(p => (p.purposeId, p.name))
            .separate
          List(
            a.eserviceId,
            a.eservice,
            a.producer,
            a.consumer,
            a.agreementId,
            purposeNames.mkString("§"),
            purposeIds.mkString("§")
          ).map(s => s"\"$s\"").mkString(",")
        }).mkString("\n")
      }
  }

  def getDescriptorsRecord(implicit ec: ExecutionContext): Future[String] = {
    logger.info("Gathering Descriptors Information")

    val header: String                       = "name,createdAt,producerId,producer,descriptorId,state,fingerprint"
    val asCsvRow: MetricDescriptor => String = (d: MetricDescriptor) =>
      s""""${d.name}","${d.createdAt}","${d.producerId}","${d.producer}","${d.descriptorId}","${d.state}","${d.fingerprint}""""
    val asCsvRows: List[MetricDescriptor] => List[String] = Functor[List].lift(asCsvRow)
    val addHeader: List[String] => List[String]           = header :: _

    val asCsv: List[MetricDescriptor] => List[String] = asCsvRows.andThen(addHeader)

    ReadModelQueries
      .getAllDescriptors(config.collections, readModel)
      .map(_.filter(_.isActive).toList)
      .map(_.map(_.toMetric))
      .map(asCsv)
      .map(_.mkString("\n"))
  }
}
