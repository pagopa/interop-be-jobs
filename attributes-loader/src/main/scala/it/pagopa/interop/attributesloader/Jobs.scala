package it.pagopa.interop.attributesloader

import cats.syntax.all._
import com.typesafe.scalalogging.LoggerTakingImplicit
import it.pagopa.interop.attributeregistrymanagement.model.persistence.JsonFormats._
import it.pagopa.interop.attributeregistrymanagement.model.persistence.attribute.PersistentAttribute
import it.pagopa.interop.attributeregistryprocess.Utils.kindToBeExcluded
import it.pagopa.interop.attributeregistryprocess.client.model.CertifiedAttributeSeed
import it.pagopa.interop.attributesloader.service.{AttributeRegistryProcessService, PartyRegistryService}
import it.pagopa.interop.commons.cqrs.service.ReadModelService
import it.pagopa.interop.commons.logging.ContextFieldsToLog
import it.pagopa.interop.commons.utils.Digester
import it.pagopa.interop.commons.utils.TypeConversions._
import org.mongodb.scala.model.Filters

import scala.concurrent.{ExecutionContext, Future}

final class Jobs(
  attributeRegistryProcessService: AttributeRegistryProcessService,
  partyRegistryService: PartyRegistryService,
  readModelService: ReadModelService
) {

  def loadCertifiedAttributes()(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog],
    ec: ExecutionContext
  ): Future[Unit] = for {
    categories <- getAllPages(50)((page, limit) =>
      partyRegistryService.getCategories(Some(page), Some(limit)).map(_.items)
    )
    _                             = logger.info(s"Categories retrieved: ${categories.size}")
    attributeSeedsCategoriesNames = categories
      .map(c =>
        CertifiedAttributeSeed(
          code = c.code,
          description = c.name, // passing the name since no description exists at party-registry-proxy
          origin = c.origin,
          name = c.name
        )
      )

    attributeSeedsCategoriesKinds = categories
      .distinctBy(_.kind)
      .filterNot(c => kindToBeExcluded.contains(c.kind)) // Including only Pubbliche Amministrazioni
      .map(c =>
        CertifiedAttributeSeed(
          code = Digester.toSha256(c.kind.getBytes),
          description = c.kind,
          origin = c.origin,
          name = c.kind
        )
      )

    attributeSeedsCategories = attributeSeedsCategoriesKinds ++ attributeSeedsCategoriesNames

    institutions <- getAllPages(1000)((page, limit) =>
      partyRegistryService.getInstitutions(Some(page), Some(limit)).map(_.items)
    )
    _                          = logger.info(s"Institutions retrieved: ${institutions.size}")
    attributeSeedsInstitutions = institutions.map(i =>
      CertifiedAttributeSeed(code = i.originId, description = i.description, origin = i.origin, name = i.description)
    )

    _ <- addNewAttributes(attributeSeedsCategories ++ attributeSeedsInstitutions)
  } yield ()

  private def addNewAttributes(attributesSeeds: Seq[CertifiedAttributeSeed])(implicit
    contexts: Seq[(String, String)],
    logger: LoggerTakingImplicit[ContextFieldsToLog],
    ec: ExecutionContext
  ): Future[Unit] = {

    // calculating the delta of attributes
    def delta(attrs: List[PersistentAttribute]): Set[CertifiedAttributeSeed] =
      attributesSeeds.foldLeft[Set[CertifiedAttributeSeed]](Set.empty)((attributesDelta, seed) =>
        attrs
          .find(persisted => seed.origin.some == persisted.origin && seed.code.some == persisted.code)
          .fold(attributesDelta + seed)(_ => attributesDelta)
      )

    // create all new attributes
    for {
      attributesfromRM <- getAll(50)(readModelService.find[PersistentAttribute]("attributes", Filters.empty(), _, _))
      deltaAttributes = delta(attributesfromRM.toList)
      _               = logger.info(s"New attributes to create: ${deltaAttributes.size}")
      // The client must log in case of errors
      _ <- Future.parCollectWithLatch(100)(deltaAttributes.toList)(seed =>
        attributeRegistryProcessService
          .createInternalCertifiedAttribute(seed)
          .map(_ => ())
          .recover(ex =>
            logger
              .error(s"Error creating attribute. Origin: ${seed.origin}, Code: ${seed.code}, Name: ${seed.name}", ex)
          )
      )
    } yield ()

  }

  private def getAllPages[T](
    limit: Int
  )(get: (Int, Int) => Future[Seq[T]])(implicit ec: ExecutionContext): Future[Seq[T]] = {
    def go(page: Int)(acc: Seq[T]): Future[Seq[T]] = {
      get(page, limit).flatMap(xs =>
        if (xs.size < limit) Future.successful(xs ++ acc)
        else go(page + 1)(xs ++ acc)
      )
    }

    go(1)(Nil)
  }

  private def getAll[T](
    limit: Int
  )(get: (Int, Int) => Future[Seq[T]])(implicit ec: ExecutionContext): Future[Seq[T]] = {
    def go(offset: Int)(acc: Seq[T]): Future[Seq[T]] = {
      get(offset, limit).flatMap(xs =>
        if (xs.size < limit) Future.successful(xs ++ acc)
        else go(offset + xs.size)(xs ++ acc)
      )
    }

    go(0)(Nil)
  }
}
