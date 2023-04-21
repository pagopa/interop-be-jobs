package it.pagopa.interop.eservicesfilegenerator.model

sealed trait State

object State {
  import spray.json._

  case object ACTIVE   extends State
  case object INACTIVE extends State

  implicit object StateFormat extends RootJsonFormat[State] {
    def write(obj: State): JsValue =
      obj match {
        case ACTIVE   => JsString("ACTIVE")
        case INACTIVE => JsString("INACTIVE")
      }

    def read(json: JsValue): State =
      json match {
        case JsString("ACTIVE")   => ACTIVE
        case JsString("INACTIVE") => INACTIVE
        case unrecognized         => deserializationError(s"State serialization error ${unrecognized.toString}")
      }
  }

  def fromValue(value: String): Either[Throwable, State] =
    value match {
      case "ACTIVE"   => Right(ACTIVE)
      case "INACTIVE" => Right(INACTIVE)
      case other      => Left(new RuntimeException(s"Unable to decode value $other"))
    }
}
