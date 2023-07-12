package it.pagopa.interop.purposesarchiver.service

import it.pagopa.interop.purposeprocess.client.model.{Purposes, PurposeVersionState, PurposeVersion}

import java.util.UUID
import scala.concurrent.Future

trait PurposeProcessService {

  def getPurposes(eServiceId: UUID, consumerId: UUID, states: Seq[PurposeVersionState], offset: Int, limit: Int)(
    implicit contexts: Seq[(String, String)]
  ): Future[Purposes]

  def archive(purposeId: UUID, versionId: UUID)(implicit contexts: Seq[(String, String)]): Future[PurposeVersion]

  def delete(purposeId: UUID, versionId: UUID)(implicit contexts: Seq[(String, String)]): Future[Unit]
}
