package it.pagopa.interop.eserviceversionsarchiver.util

import it.pagopa.interop.agreementmanagement.model.agreement.PersistentAgreement
import it.pagopa.interop.agreementmanagement.model.persistence.JsonFormats.paFormat
import it.pagopa.interop.commons.cqrs.service.MongoDbReadModelService
import it.pagopa.interop.eserviceversionsarchiver.ApplicationConfiguration._
import org.mongodb.scala.bson.conversions.Bson

import scala.concurrent.Future

object ReadModelQueries {

  def getAllAgreements(
    rm: MongoDbReadModelService,
    descriptorAndEserviceFilter: Bson
  ): Future[Seq[PersistentAgreement]] =
    getAll(100)(rm.find[PersistentAgreement](agreementsCollection, descriptorAndEserviceFilter, _, _))

  private def getAll[T](limit: Int)(get: (Int, Int) => Future[Seq[T]]): Future[Seq[T]] = {
    def go(offset: Int)(acc: Seq[T]): Future[Seq[T]] = get(offset, limit).flatMap(xs =>
      if (xs.size < limit) Future.successful(xs ++ acc)
      else go(offset + xs.size)(xs ++ acc)
    )
    go(0)(Nil)
  }
}
