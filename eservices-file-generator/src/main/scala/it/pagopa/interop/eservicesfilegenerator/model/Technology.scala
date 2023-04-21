package it.pagopa.interop.eservicesfilegenerator.model

sealed trait Technology

object Technology {
  import spray.json._

  case object REST extends Technology
  case object SOAP extends Technology

  implicit object TechnologyFormat extends RootJsonFormat[Technology] {
    def write(obj: Technology): JsValue =
      obj match {
        case REST => JsString("REST")
        case SOAP => JsString("SOAP")
      }

    def read(json: JsValue): Technology =
      json match {
        case JsString("REST") => REST
        case JsString("SOAP") => SOAP
        case unrecognized     => deserializationError(s"Technology serialization error ${unrecognized.toString}")
      }
  }

  def fromValue(value: String): Either[Throwable, Technology] =
    value match {
      case "REST" => Right(REST)
      case "SOAP" => Right(SOAP)
      case other  => Left(new RuntimeException(s"Unable to decode value $other"))
    }
}
