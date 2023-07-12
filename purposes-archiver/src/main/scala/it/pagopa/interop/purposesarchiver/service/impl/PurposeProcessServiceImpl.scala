package it.pagopa.interop.purposesarchiver.service.impl

import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.withHeaders
import it.pagopa.interop.purposesarchiver.service.PurposeProcessService
import it.pagopa.interop.purposeprocess.client.api.PurposeApi
import it.pagopa.interop.purposeprocess.client.invoker.{ApiRequest, BearerToken, ApiInvoker => PurposeProcessInvoker}
import it.pagopa.interop.purposeprocess.client.model.{Purpose, Purposes, PurposeVersionState, PurposeVersion}

import java.util.UUID
import scala.concurrent.{Future, ExecutionContext}

final case class PurposeProcessServiceImpl(invoker: PurposeProcessInvoker, api: PurposeApi)
    extends PurposeProcessService {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  def getAllPurposes(eServiceId: UUID, consumerId: UUID, states: Seq[PurposeVersionState])(implicit
    contexts: Seq[(String, String)],
    ec: ExecutionContext
  ): Future[Seq[Purpose]] = {

    def getPurposesFrom(offset: Int): Future[Seq[Purpose]] =
      getPurposes(eServiceId = eServiceId, consumerId = consumerId, states = states, offset = offset, limit = 50).map(
        _.results
      )

    def go(start: Int)(as: Seq[Purpose]): Future[Seq[Purpose]] =
      getPurposesFrom(start).flatMap(prps =>
        if (prps.size < 50) Future.successful(as ++ prps) else go(start + 50)(as ++ prps)
      )

    go(0)(Nil)
  }

  override def getPurposes(
    eServiceId: UUID,
    consumerId: UUID,
    states: Seq[PurposeVersionState],
    offset: Int,
    limit: Int
  )(implicit contexts: Seq[(String, String)]): Future[Purposes] =
    withHeaders { (bearerToken, correlationId, ip) =>
      val request: ApiRequest[Purposes] = api.getPurposes(
        xCorrelationId = correlationId,
        name = None,
        eservicesIds = Seq(eServiceId),
        consumersIds = Seq(consumerId),
        producersIds = Seq.empty,
        states = states,
        excludeDraft = None,
        offset = 0,
        limit = 50,
        xForwardedFor = ip
      )(BearerToken(bearerToken))
      invoker.invoke(request, s"Retrieving purposes with EServiceId $eServiceId and consumerId $consumerId")
    }

  override def archive(purposeId: UUID, versionId: UUID)(implicit
    contexts: Seq[(String, String)]
  ): Future[PurposeVersion] =
    withHeaders { (bearerToken, correlationId, ip) =>
      val request: ApiRequest[PurposeVersion] = api.archivePurposeVersion(
        xCorrelationId = correlationId,
        purposeId = purposeId,
        versionId = versionId,
        xForwardedFor = ip
      )(BearerToken(bearerToken))
      invoker.invoke(request, s"Archive purpose $purposeId with version $versionId")
    }

  override def delete(purposeId: UUID, versionId: UUID)(implicit contexts: Seq[(String, String)]): Future[Unit] =
    withHeaders { (bearerToken, correlationId, ip) =>
      val request: ApiRequest[Unit] = api.deletePurposeVersion(
        xCorrelationId = correlationId,
        purposeId = purposeId,
        versionId = versionId,
        xForwardedFor = ip
      )(BearerToken(bearerToken))
      invoker.invoke(request, s"Delete purpose $purposeId with version $versionId")
    }
}
