package it.pagopa.interop.tenantsattributeschecker.service

import it.pagopa.interop.attributeregistryprocess.client.model.Attribute

import java.util.UUID
import scala.concurrent.Future

trait AttributeRegistryProcessService {

  def getAttributeById(attributeId: UUID)(implicit contexts: Seq[(String, String)]): Future[Attribute]
}
