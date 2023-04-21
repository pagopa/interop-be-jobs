package it.pagopa.interop.eservicesfilegenerator.model

import spray.json.DefaultJsonProtocol._
import it.pagopa.interop.catalogmanagement.{model => DependencyCatalog}
import it.pagopa.interop.catalogmanagement.model.persistence.JsonFormats._
import it.pagopa.interop.commons.utils.SprayCommonFormats.{offsetDateTimeFormat, uuidFormat}
import spray.json._
import java.time.OffsetDateTime
import java.util.UUID

final case class EServiceDB(
  id: UUID,
  name: String,
  technology: DependencyCatalog.CatalogItemTechnology,
  producerName: String,
  descriptors: Seq[DescriptorDB]
)

final case class DescriptorDB(
  id: UUID,
  state: DependencyCatalog.CatalogDescriptorState,
  serverUrls: Seq[String],
  createdAt: OffsetDateTime,
  version: String
)

object EServiceDB {
  implicit val descriptorDBFormat: RootJsonFormat[DescriptorDB] = jsonFormat5(DescriptorDB.apply)
  implicit val eServiceDBFormat: RootJsonFormat[EServiceDB]     = jsonFormat5(EServiceDB.apply)
}
