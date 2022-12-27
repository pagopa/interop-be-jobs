package it.pagopa.interop.metricsreportgenerator.util

import it.pagopa.interop.catalogmanagement.model.CatalogItem
import it.pagopa.interop.catalogmanagement.model.persistence.JsonFormats._
import it.pagopa.interop.commons.cqrs.service.ReadModelService
import it.pagopa.interop.commons.utils.TypeConversions.OptionOps
import it.pagopa.interop.metricsreportgenerator.util.Error.TenantNotFound
import it.pagopa.interop.tenantmanagement.model.persistence.JsonFormats._
import it.pagopa.interop.tenantmanagement.model.tenant.PersistentTenant
import org.mongodb.scala.model.Filters

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

object ReadModelQueries {
  def getEServices(offset: Int, limit: Int)(implicit
    ec: ExecutionContext,
    readModelService: ReadModelService
  ): Future[Seq[CatalogItem]] =
    readModelService.find[CatalogItem](ApplicationConfiguration.catalogCollection, Filters.empty(), offset, limit)

  def getTenant(
    tenantId: UUID
  )(implicit ec: ExecutionContext, readModelService: ReadModelService): Future[PersistentTenant] =
    readModelService
      .findOne[PersistentTenant](ApplicationConfiguration.tenantCollection, Filters.eq("data.id", tenantId.toString))
      .flatMap(_.toFuture(TenantNotFound(tenantId)))
}
