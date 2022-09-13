package it.pagopa.interop.tenantscertifiedattributesupdater

import cats.implicits._
import org.mongodb.scala.Document
import spray.json.{JsonReader, _}
package object repository {

  case object NoDataFoundInDocument extends Throwable("No data field found in the document")

  def extractData[T: JsonReader](document: Document): Either[Throwable, T] =
    document
      .toJson()
      .parseJson
      .asJsObject
      .fields
      .get("data")
      .toRight(NoDataFoundInDocument)
      .flatMap(data => Either.catchNonFatal(data.convertTo[T]))

}
