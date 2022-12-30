package it.pagopa.interop.metricsreportgenerator.report

import it.pagopa.interop.metricsreportgenerator.models.{Agreement, Purpose}
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
  purposes: Seq[String]
) {
  def stringify: String =
    List(activationDate, agreementId, eserviceId, eservice, producer, consumer, consumerId, purposes.mkString("|"))
      .mkString(",")
}

object AgreementRecord {

  implicit val format: RootJsonFormat[AgreementRecord] = jsonFormat8(AgreementRecord.apply)

  def create(activeAgreement: Agreement, purposeIds: Seq[String]): AgreementRecord = {
    AgreementRecord(
      activationDate = activeAgreement.activationDate,
      agreementId = activeAgreement.agreementId,
      eserviceId = activeAgreement.eserviceId,
      eservice = activeAgreement.eservice,
      producer = activeAgreement.producer,
      consumer = activeAgreement.consumer,
      consumerId = activeAgreement.consumerId,
      purposes = purposeIds
    )
  }

  def join(agreements: Seq[Agreement], purposes: Seq[Purpose]): Seq[AgreementRecord] =
    agreements.map { agreement =>
      val purposeIds: Seq[String] = purposes
        .filter(purpose => purpose.consumerId == agreement.consumerId && purpose.eserviceId == agreement.eserviceId)
        .map(_.purposeId)
      AgreementRecord.create(agreement, purposeIds)
    }

  def csv(agreementRecords: Seq[AgreementRecord]): Seq[String] = {
    val header = agreementRecords.headOption.map(_.productElementNames.mkString(",")).getOrElse("")
    (header :: agreementRecords.toList.map(_.stringify))
  }
}
