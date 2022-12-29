package it.pagopa.interop.metricsreportgenerator.models

import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

final case class ActiveAgreement(
  activationDate: String,
  agreementId: String,
  eserviceId: String,
  eservice: String,
  producer: String,
  consumer: String
)

object ActiveAgreement {
  implicit val format: RootJsonFormat[ActiveAgreement] = jsonFormat6(ActiveAgreement.apply)
}
