package it.pagopa.interop.tenantscertifiedattributesupdater

import org.mongodb.scala.Document
import spray.json.{JsonReader, _}

package object repository {

  def extractData[T: JsonReader](document: Document): T = {
    val fields = document.toJson().parseJson.asJsObject.getFields("data")
    fields match {
      case data :: Nil => data.convertTo[T]

    }
  }
}
