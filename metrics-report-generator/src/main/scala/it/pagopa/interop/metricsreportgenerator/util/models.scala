package it.pagopa.interop.metricsreportgenerator.util.models

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

final case class Purpose(purposeId: String, consumerId: String, eserviceId: String, name: String)

object Purpose {
  implicit val format: RootJsonFormat[Purpose] = jsonFormat4(Purpose.apply)
}

final case class Descriptor(name: String, createdAt: String, producerId: String, descriptorId: String, state: String)

object Descriptor {
  implicit val format: RootJsonFormat[Descriptor] = jsonFormat5(Descriptor.apply)
}
