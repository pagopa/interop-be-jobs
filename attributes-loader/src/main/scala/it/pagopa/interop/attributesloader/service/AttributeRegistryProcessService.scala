package it.pagopa.interop.attributesloader.service

import it.pagopa.interop.attributeregistryprocess.client.model.{Attribute, InternalCertifiedAttributeSeed}

import scala.concurrent.Future

trait AttributeRegistryProcessService {

  def createInternalCertifiedAttribute(seed: InternalCertifiedAttributeSeed)(implicit
    contexts: Seq[(String, String)]
  ): Future[Attribute]

}
