package it.pagopa.interop.metricsreportgenerator.util

import cats.Functor
import cats.syntax.all._
import com.typesafe.scalalogging.LoggerTakingImplicit
import it.pagopa.interop.commons.cqrs.service.ReadModelService
import it.pagopa.interop.commons.logging._
import it.pagopa.interop.metricsreportgenerator.util.models.{
  SheetStyle,
  Agreement,
  Purpose,
  Descriptor,
  MetricDescriptor
}

import spoiwo.model._

import scala.concurrent.{ExecutionContext, Future}

class Jobs(config: Configuration, readModel: ReadModelService)(implicit
  logger: LoggerTakingImplicit[ContextFieldsToLog],
  context: ContextFieldsToLog
) {

  def getAgreements(implicit ec: ExecutionContext): Future[List[Agreement]] = ReadModelQueries
    .getAllActiveAgreements(config.collections, readModel)
    .map(_.toList)

  def getPurposes(implicit ec: ExecutionContext): Future[List[Purpose]] =
    ReadModelQueries.getAllPurposes(config.collections, readModel).map(_.toList)

  def getActiveDescriptors(implicit ec: ExecutionContext): Future[List[Descriptor]] = ReadModelQueries
    .getAllDescriptors(config.collections, readModel)
    .map(_.filter(_.isActive).toList)

  def getAgreementRecord(agreements: List[Agreement], purposes: List[Purpose]): String = {
    logger.info("Gathering Agreements Information")
    val header = "eserviceId,eservice,producerId,producer,consumerId,consumer,agreementId,state,purposes,purposeIds"
    (header :: agreements.map { a =>
      val (purposeIds, purposeNames): (Seq[String], Seq[String]) = purposes
        .filter(p => p.consumerId == a.consumerId && p.eserviceId == a.eserviceId)
        .map(p => (p.purposeId, p.name))
        .separate
      List(
        a.eserviceId,
        a.eservice,
        a.producerId,
        a.producer,
        a.consumerId,
        a.consumer,
        a.agreementId,
        a.state,
        purposeNames.mkString("§"),
        purposeIds.mkString("§")
      ).map(s => s"\"$s\"").mkString(",")
    }).mkString("\n")
  }

  def getAgreementSheet(agreements: List[Agreement], purposes: List[Purpose]): Sheet = {
    logger.info("Gathering Agreements Information")
    val headerList = List(
      "EserviceId",
      "Eservice",
      "Producer",
      "ProducerId",
      "Consumer",
      "ConsumerId",
      "AgreementId",
      "Purposes",
      "PurposeIds"
    )
    val headerRow  =
      Row(style = SheetStyle.headerStyle).withCellValues(headerList)

    val columns = (0 until (headerList.size)).toList
      .map(index => Column(index = index, style = CellStyle(font = Font(bold = true)), autoSized = true))

    val rows = agreements.map { a =>
      val (purposeIds, purposeNames): (Seq[String], Seq[String]) = purposes
        .filter(p => p.consumerId == a.consumerId && p.eserviceId == a.eserviceId)
        .map(p => (p.purposeId, p.name))
        .separate

      Row(style = SheetStyle.rowStyle).withCellValues(
        a.eserviceId,
        a.eservice,
        a.producer,
        a.producerId,
        a.consumer,
        a.consumerId,
        a.agreementId,
        purposeNames.mkString(", "),
        purposeIds.mkString(", ")
      )
    }

    Sheet(name = "Agreements", rows = headerRow :: rows, columns = columns)
  }

  def getDescriptorsRecord(descriptors: List[Descriptor])(implicit ec: ExecutionContext): Future[String] = {
    logger.info("Gathering Descriptors Information")

    val header: String = "name,createdAt,producerId,producer,descriptorId,state,fingerprint,tokenDuration"
    val asCsvRow: MetricDescriptor => String              = (d: MetricDescriptor) =>
      s""""${d.name}","${d.createdAt}","${d.producerId}","${d.producer}","${d.descriptorId}","${d.state}","${d.fingerprint}","${d.tokenDuration}""""
    val asCsvRows: List[MetricDescriptor] => List[String] = Functor[List].lift(asCsvRow)
    val addHeader: List[String] => List[String]           = header :: _

    val asCsv: List[MetricDescriptor] => List[String] = asCsvRows.andThen(addHeader)

    descriptors
      .traverse(_.toMetric)
      .map(asCsv)
      .map(_.mkString("\n"))
  }

  def getDescriptorsSheet(descriptors: List[Descriptor])(implicit ec: ExecutionContext): Future[Sheet] = {
    logger.info("Gathering Descriptors Information")

    val headerList = List("Name", "CreatedAt", "ProducerId", "Producer", "DescriptorId", "State", "Fingerprint")
    val headerRow  =
      Row(style = SheetStyle.headerStyle).withCellValues(headerList)

    val columns = (0 until (headerList.size)).toList
      .map(index => Column(index = index, style = CellStyle(font = Font(bold = true)), autoSized = true))

    for {
      metricDescriptors <- descriptors.traverse(_.toMetric)
      rows = metricDescriptors
        .map(descriptor =>
          Row(style = SheetStyle.rowStyle).withCellValues(
            descriptor.name,
            descriptor.createdAt,
            descriptor.producerId,
            descriptor.producer,
            descriptor.descriptorId,
            descriptor.state,
            descriptor.fingerprint
          )
        )
        .toList
    } yield Sheet(name = "Descriptors", rows = headerRow :: rows, columns = columns)
  }
}
