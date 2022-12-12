package it.pagopa.interop.metricsreportgenerator.repository

import it.pagopa.interop.catalogmanagement.model.CatalogItem

import scala.concurrent.Future

trait CatalogRepository {
  def getEServices: Future[Seq[Either[Throwable, CatalogItem]]]
}
