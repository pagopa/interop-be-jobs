package it.pagopa.interop.attributesloader.service

import it.pagopa.interop.attributeregistryprocess.client.model.{Attribute, CertifiedAttributeSeed}

import scala.concurrent.Future

trait AttributeRegistryProcessService {

  def createInternalCertifiedAttribute(attributeSeed: CertifiedAttributeSeed)(implicit
    contexts: Seq[(String, String)]
  ): Future[Attribute]

}
