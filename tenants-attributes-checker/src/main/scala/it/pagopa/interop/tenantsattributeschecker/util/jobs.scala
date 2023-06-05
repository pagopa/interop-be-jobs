package it.pagopa.interop.tenantsattributeschecker.util

import it.pagopa.interop.certifiedMailSender.InteropEnvelope
import it.pagopa.interop.commons.utils.TypeConversions.OptionOps
import it.pagopa.interop.commons.utils.service.UUIDSupplier
import it.pagopa.interop.tenantmanagement.model.tenant.{
  PersistentTenant,
  PersistentVerificationRenewal,
  PersistentVerifiedAttribute
}
import it.pagopa.interop.tenantsattributeschecker.ApplicationConfiguration.{
  actorSystem,
  blockingEc,
  certifiedMailQueueName,
  context
}
import it.pagopa.interop.tenantsattributeschecker.service.impl.{
  AgreementProcessServiceImpl,
  MailTemplate,
  PartyProcessServiceImpl,
  QueueServiceImpl,
  TenantProcessServiceImpl,
  interopEnvelopFormat
}
import it.pagopa.interop.tenantsattributeschecker.service.{
  AgreementProcessService,
  PartyProcessService,
  QueueService,
  TenantProcessService
}
import it.pagopa.interop.tenantsattributeschecker.util.errors._
import it.pagopa.interop.commons.utils.TypeConversions._

import java.util.UUID
import scala.concurrent.Future

object jobs {

  private val agreementProcess: AgreementProcessService =
    AgreementProcessServiceImpl(blockingEc)
  private val tenantProcess: TenantProcessService       =
    TenantProcessServiceImpl(blockingEc)
  private val queueService: QueueService                = new QueueServiceImpl(certifiedMailQueueName)
  private val partyProcessService: PartyProcessService  = new PartyProcessServiceImpl
  private val expiredMailTemplate: MailTemplate         = MailTemplate.expired()

  def applyStrategy(tenants: List[PersistentTenant]): Future[Unit] = {

    Future
      .traverse(tenants) { tenant =>
        Future.traverse(tenant.attributes.collect { case v: PersistentVerifiedAttribute => v }) { attribute =>
          Future.traverse(attribute.verifiedBy) { verifiedBy =>
            for {
              _ <- verifiedBy.renewal match {
                case PersistentVerificationRenewal.AUTOMATIC_RENEWAL    =>
                  tenantProcess.updateVerifiedAttributeExtensionDate(tenant.id, attribute.id, verifiedBy.id)
                case PersistentVerificationRenewal.REVOKE_ON_EXPIRATION =>
                  agreementProcess.computeAgreementsByAttribute(tenant.id, attribute.id)
              }
              _ <- sendEnvelope(attribute.id, tenant, verifiedBy.renewal, expiredMailTemplate)
            } yield ()
          }
        }
      }
      .map(_ => ())
  }

  def sendEnvelope(
    attributeId: UUID,
    tenant: PersistentTenant,
    renewal: PersistentVerificationRenewal,
    mailTemplate: MailTemplate
  ): Future[Unit] = {
    val envelopeId: UUID = UUIDSupplier.get()

    def createBody: String = mailTemplate.body.interpolate(
      Map(
        "ifRevoke"    -> {
          if (renewal == PersistentVerificationRenewal.REVOKE_ON_EXPIRATION) "non "
          else ""
        },
        "attributeId" -> attributeId.toString
      )
    )

    val subject: String = mailTemplate.subject.interpolate(Map("attributeId" -> attributeId.toString))

    val envelope: Future[InteropEnvelope] = for {
      tenantSelfcareId <- tenant.selfcareId.toFuture(SelfcareIdNotFound(tenant.id))
      tenant           <- partyProcessService.getInstitution(tenantSelfcareId)
    } yield InteropEnvelope(
      id = envelopeId,
      recipients = List(tenant.digitalAddress),
      subject = subject,
      body = createBody,
      attachments = List.empty
    )

    envelope.flatMap(queueService.send[InteropEnvelope]).map(_ => ())
  }
}
