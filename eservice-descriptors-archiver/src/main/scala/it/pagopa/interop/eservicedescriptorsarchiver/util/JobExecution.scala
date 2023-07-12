package it.pagopa.interop.eservicedescriptorsarchiver.util

import io.circe.parser._
import it.pagopa.interop.agreementmanagement.model.agreement.{Archived, PersistentAgreement}
import it.pagopa.interop.agreementmanagement.model.persistence.JsonFormats.paFormat
import it.pagopa.interop.catalogmanagement.model.persistence.JsonFormats.ciFormat
import it.pagopa.interop.catalogmanagement.model.{CatalogItem, Deprecated, Published, Suspended}
import it.pagopa.interop.commons.cqrs.service.MongoDbReadModelService
import it.pagopa.interop.commons.utils.TypeConversions._
import it.pagopa.interop.eservicedescriptorsarchiver.ApplicationConfiguration._
import it.pagopa.interop.eservicedescriptorsarchiver.service.CatalogProcessService
import it.pagopa.interop.eservicedescriptorsarchiver.util.errors._
import org.mongodb.scala.model.Filters
import spray.json.BasicFormats

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

final case class JobExecution(readModelService: MongoDbReadModelService, catalogProcess: CatalogProcessService)(implicit
  ec: ExecutionContext,
  context: List[(String, String)]
) extends BasicFormats {

  def archiveEservice(message: String): Future[Unit] =
    for {
      json        <- parse(message).toFuture
      agreementId <- json.hcursor.get[String]("agreementId").toFuture
      _           <- archiveEserviceInternal(agreementId)
    } yield ()

  private def archiveEserviceInternal(agreementId: String): Future[Unit] = {
    for {
      maybeAgreement             <- readModelService
        .findOne[PersistentAgreement](agreementsCollection, Filters.eq("data.id", agreementId))
      (eServiceId, descriptorId) <- maybeAgreement
        .toFuture(AgreementNotFound(agreementId))
        .map(agreement => (agreement.eserviceId, agreement.descriptorId))
      descriptorAndEserviceFilter = Filters.and(
        Filters.eq("data.descriptorId", descriptorId.toString),
        Filters.eq("data.eserviceId", eServiceId.toString)
      )
      relatingAgreements <- ReadModelQueries(readModelService).getAllAgreements(descriptorAndEserviceFilter)
      allArchived = relatingAgreements.forall(_.state == Archived)
      _                  <-        if (allArchived) archiveEserviceDescriptor(eServiceId, descriptorId)
        else Future.unit
    } yield ()
  }

  private def archiveEserviceDescriptor(eServiceId: UUID, descriptorId: UUID): Future[Unit] =
    for {
      maybeEservice <- readModelService
        .findOne[CatalogItem](eservicesCollection, Filters.eq("data.id", eServiceId.toString))
      eservice      <- maybeEservice.toFuture(EserviceNotFound(eServiceId))
      descriptor    <- eservice.descriptors.find(_.id == descriptorId).toFuture(DescriptorNotFound(descriptorId))
      _             <- {
        descriptor.state match {
          case Deprecated => catalogProcess.archiveDescriptor(eServiceId, descriptorId)
          case Suspended  =>
            val newerDescriptorExists = eservice.descriptors
              .exists(actualDescriptor =>
                (actualDescriptor.state == Published || actualDescriptor.state == Suspended)
                  && actualDescriptor.version.toInt > descriptor.version.toInt
              )
            if (newerDescriptorExists) catalogProcess.archiveDescriptor(eServiceId, descriptorId)
            else Future.unit
          case _          => Future.unit
        }
      }
    } yield ()
}
