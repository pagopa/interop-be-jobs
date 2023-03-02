package it.pagopa.interop.metricsreportgenerator.report

import it.pagopa.interop.metricsreportgenerator.util.models.{Agreement, Purpose}
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

final case class AgreementRecord(
  activationDate: String,
  agreementId: String,
  eserviceId: String,
  eservice: String,
  producer: String,
  consumer: String,
  consumerId: String,
  purposes: Seq[(String, String)]
)

object AgreementRecord {

  implicit val format: RootJsonFormat[AgreementRecord] = jsonFormat8(AgreementRecord.apply)

  def create(activeAgreement: Agreement, purposes: Seq[(String, String)]): AgreementRecord = AgreementRecord(
    activationDate = activeAgreement.activationDate,
    agreementId = activeAgreement.agreementId,
    eserviceId = activeAgreement.eserviceId,
    eservice = activeAgreement.eservice,
    producer = activeAgreement.producer,
    consumer = activeAgreement.consumer,
    consumerId = activeAgreement.consumerId,
    purposes = purposes
  )

  def join(agreements: Seq[Agreement], purposes: Seq[Purpose]): Seq[AgreementRecord] = agreements.map { agreement =>
    val purposeIds: Seq[(String, String)] = purposes
      .filter(purpose => purpose.consumerId == agreement.consumerId && purpose.eserviceId == agreement.eserviceId)
      .map(p => (p.purposeId, p.name))
    AgreementRecord.create(agreement, purposeIds)
  }

  private def stringify(a: AgreementRecord): String = List(
    a.eservice,
    a.producer,
    a.consumer,
    a.activationDate,
    a.purposes.map(_._2).mkString("|"),
    a.agreementId,
    a.eserviceId,
    a.consumerId,
    a.purposes.map(_._1).mkString("|")
  ).mkString(",")

  def csv(agreementRecords: Seq[AgreementRecord]): List[String] = agreementRecords.toList
    .map(stringify)
    .prepended("eservice,producer,consumer,activationDate,purposes,agreementId,eserviceId,consumerId,purposeIds")

}
