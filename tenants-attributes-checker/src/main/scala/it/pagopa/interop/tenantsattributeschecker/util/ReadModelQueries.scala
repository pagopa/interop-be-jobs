package it.pagopa.interop.tenantsattributeschecker.util

import it.pagopa.interop.agreementmanagement.model.agreement.PersistentAgreement
import it.pagopa.interop.agreementmanagement.model.persistence.JsonFormats.paFormat
import it.pagopa.interop.commons.cqrs.service.ReadModelService
import it.pagopa.interop.tenantmanagement.model.persistence.JsonFormats.ptFormat
import it.pagopa.interop.tenantmanagement.model.tenant.PersistentTenant
import it.pagopa.interop.tenantsattributeschecker.ApplicationConfiguration.{
  agreementsCollection,
  dateTimeSupplier,
  tenantsCollection
}
import org.mongodb.scala.model.Filters._

import scala.concurrent.{ExecutionContext, Future}

object ReadModelQueries {

  def getExpiredAttributesTenants(rm: ReadModelService)(implicit ec: ExecutionContext): Future[Seq[PersistentTenant]] =
    getAll(100)(
      rm.find[PersistentTenant](
        tenantsCollection,
        lte("data.attributes.verifiedBy.extensionDate", dateTimeSupplier.get().toString),
        _,
        _
      )
    )

  def getExpiredAttributesAgreements(rm: ReadModelService, expiredAttributes: Seq[String])(implicit
    ec: ExecutionContext
  ): Future[Seq[PersistentAgreement]] =
    getAll(100)(
      rm.find[PersistentAgreement](agreementsCollection, in("data.verifiedAttributes.id", expiredAttributes), _, _)
    )

  private def getAll[T](
    limit: Int
  )(get: (Int, Int) => Future[Seq[T]])(implicit ex: ExecutionContext): Future[Seq[T]] = {
    def go(offset: Int)(acc: Seq[T]): Future[Seq[T]] = get(offset, limit).flatMap(xs =>
      if (xs.size < limit) Future.successful(xs ++ acc)
      else go(offset + xs.size)(xs ++ acc)
    )
    go(0)(Nil)
  }

}
