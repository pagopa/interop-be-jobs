package it.pagopa.interop.tenantscertifiedattributesupdater

import it.pagopa.interop.commons.cqrs.model.CqrsMetadata
import org.mongodb.scala.Document
import spray.json.{JsonReader, _}

package object repository {

  def extractData[T: JsonReader](document: Document): T = {
    val fields = document.toJson().parseJson.asJsObject.getFields("data", "metadata")
    fields match {
      case data :: metadata :: Nil =>
        val cqrsMetadata = metadata.convertTo[CqrsMetadata]

        assert(cqrsMetadata.sourceEvent.persistenceId.nonEmpty)
        assert(cqrsMetadata.sourceEvent.sequenceNr >= 0)
        assert(cqrsMetadata.sourceEvent.timestamp > 0)

        data.convertTo[T]
      case _ => throw new RuntimeException(s"Unexpected number of fields ${fields.size}. Content: $fields")
    }
  }
}
