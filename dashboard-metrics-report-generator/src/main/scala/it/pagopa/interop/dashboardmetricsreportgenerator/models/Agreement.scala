package it.pagopa.interop.dashboardmetricsreportgenerator.models

import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

final case class Agreement(
  activationDate: String,
  agreementId: String,
  eserviceId: String,
  eservice: String,
  producer: String,
  consumer: String,
  consumerId: String
)

object Agreement {
  implicit val format: RootJsonFormat[Agreement] = jsonFormat7(Agreement.apply)
}
