package it.pagopa.interop.eservicedescriptorsarchiver.service.impl

import akka.actor.ActorSystem
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.catalogprocess.client.api.{EnumsSerializers, ProcessApi}
import it.pagopa.interop.catalogprocess.client.invoker.{ApiInvoker, BearerToken}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.TypeConversions.EitherOps
import it.pagopa.interop.commons.utils.extractHeaders
import it.pagopa.interop.eservicedescriptorsarchiver.ApplicationConfiguration
import it.pagopa.interop.eservicedescriptorsarchiver.service.CatalogProcessService

import java.util.UUID
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

final case class CatalogProcessServiceImpl(blockingEc: ExecutionContextExecutor)(implicit ec: ExecutionContext)
    extends CatalogProcessService {

  private val actorSystem: ActorSystem = ActorSystem("catalogProcessActorSystem")
  private val invoker: ApiInvoker      = ApiInvoker(EnumsSerializers.all, blockingEc)(actorSystem.classicSystem)
  private val api: ProcessApi          = ProcessApi(ApplicationConfiguration.catalogProcessURL)

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  override def archiveDescriptor(eServiceId: UUID, descriptorId: UUID)(implicit
    contexts: Seq[(String, String)]
  ): Future[Unit] = {
    for {
      (bearerToken, correlationId) <- extractHeaders(contexts).toFuture
      request = api.archiveDescriptor(
        xCorrelationId = correlationId,
        eServiceId = eServiceId,
        descriptorId = descriptorId
      )(BearerToken(bearerToken))
      result <- invoker
        .invoke(request, s"ArchiveDescriptor triggered for eService $eServiceId and descriptor $descriptorId")
    } yield result
  }
}
