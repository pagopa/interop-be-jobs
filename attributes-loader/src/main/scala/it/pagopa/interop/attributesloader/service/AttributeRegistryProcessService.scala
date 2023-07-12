package it.pagopa.interop.attributesloader.service

import it.pagopa.interop.attributeregistryprocess.client.model.{Attribute, AttributeSeed}

import scala.concurrent.Future

trait AttributeRegistryProcessService {

  def createAttribute(attributeSeed: AttributeSeed)(implicit contexts: Seq[(String, String)]): Future[Attribute]

}
