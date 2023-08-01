package it.pagopa.interop.eservicedescriptorsarchiver.service

import java.util.UUID
import scala.concurrent.Future

trait CatalogProcessService {

  def archiveDescriptor(eServiceId: UUID, descriptorId: UUID)(implicit contexts: Seq[(String, String)]): Future[Unit]
}
