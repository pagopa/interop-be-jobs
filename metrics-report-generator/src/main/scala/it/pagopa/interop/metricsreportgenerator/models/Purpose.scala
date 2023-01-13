package it.pagopa.interop.metricsreportgenerator.models

import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

final case class Purpose(purposeId: String, consumerId: String, eserviceId: String, name: String)

object Purpose {
  implicit val format: RootJsonFormat[Purpose] = jsonFormat4(Purpose.apply)
}
