package it.pagopa.interop.eserviceversionsarchiver.util

import io.circe.parser._
import it.pagopa.interop.agreementmanagement.model.agreement.{Archived, PersistentAgreement}
import it.pagopa.interop.agreementmanagement.model.persistence.JsonFormats.paFormat
import it.pagopa.interop.catalogmanagement.model.persistence.JsonFormats.ciFormat
import it.pagopa.interop.catalogmanagement.model.{CatalogItem, Deprecated, Published, Suspended}
import it.pagopa.interop.commons.cqrs.service.MongoDbReadModelService
import it.pagopa.interop.commons.utils.TypeConversions._
import it.pagopa.interop.eserviceversionsarchiver.ApplicationConfiguration._
import it.pagopa.interop.eserviceversionsarchiver.service.CatalogProcessService
import it.pagopa.interop.eserviceversionsarchiver.util.ReadModelQueries.getAllAgreements
import it.pagopa.interop.eserviceversionsarchiver.util.errors._
import org.mongodb.scala.model.Filters
import spray.json.BasicFormats

import scala.concurrent.Future

final case class JobExecution(readModelService: MongoDbReadModelService, catalogProcess: CatalogProcessService)
    extends BasicFormats {

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
      relatingAgreements <- getAllAgreements(readModelService, descriptorAndEserviceFilter)
      _                  <- {
        val allArchived = relatingAgreements.forall(_.state == Archived)
        if (allArchived) Future.unit
        else Future.failed(NonArchivableDescriptorException(eServiceId, descriptorId))
      }
      maybeEservice <- readModelService.findOne[CatalogItem](eservicesCollection, Filters.eq("data.id", eServiceId.toString))
      eservice      <- maybeEservice.toFuture(EserviceNotFound(eServiceId))
      descriptor    <- eservice.descriptors.find(_.id == descriptorId).toFuture(DescriptorNotFound(descriptorId))
      _             <- {
        descriptor.state match {
          case Deprecated => Future.unit
          case Suspended  =>
            val newerDescriptorExists = eservice.descriptors
              .exists(actualDescriptor =>
                (actualDescriptor.state == Published || actualDescriptor.state == Suspended)
                  && actualDescriptor.version.toInt > descriptor.version.toInt
              )
            if (newerDescriptorExists) Future.unit
            else Future.failed(NonArchivableDescriptorException(eServiceId, descriptorId))
          case _          => Future.failed(NonArchivableDescriptorException(eServiceId, descriptorId))
        }
      }
      _             <- catalogProcess.archiveDescriptor(eServiceId, descriptorId)
    } yield ()
  }
}
