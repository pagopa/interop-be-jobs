package it.pagopa.interop.tenantsattributeschecker.util

import it.pagopa.interop.certifiedMailSender.InteropEnvelope
import it.pagopa.interop.commons.utils.TypeConversions.OptionOps
import it.pagopa.interop.commons.utils.service.UUIDSupplier
import it.pagopa.interop.tenantmanagement.model.tenant.{
  PersistentTenant,
  PersistentTenantVerifier,
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
  AttributeRegistryProcessServiceImpl,
  MailTemplate,
  PartyProcessServiceImpl,
  QueueServiceImpl,
  TenantProcessServiceImpl,
  interopEnvelopFormat
}
import it.pagopa.interop.tenantsattributeschecker.service.{
  AgreementProcessService,
  AttributeRegistryProcessService,
  PartyProcessService,
  QueueService,
  TenantProcessService
}
import it.pagopa.interop.tenantsattributeschecker.util.errors._
import it.pagopa.interop.commons.utils.TypeConversions._

import java.util.UUID
import scala.concurrent.Future

object jobs {

  private val agreementProcess: AgreementProcessService                 = AgreementProcessServiceImpl(blockingEc)
  private val tenantProcess: TenantProcessService                       = TenantProcessServiceImpl(blockingEc)
  private val attributeRegistryProcess: AttributeRegistryProcessService =
    AttributeRegistryProcessServiceImpl(blockingEc)
  private val queueService: QueueService                                = new QueueServiceImpl(certifiedMailQueueName)
  private val partyProcessService: PartyProcessService                  = new PartyProcessServiceImpl
  private val expiredMailTemplate: MailTemplate                         = MailTemplate.expired()

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
              _ <- sendEnvelope(attribute.id, tenant, verifiedBy, expiredMailTemplate)
            } yield ()
          }
        }
      }
      .map(_ => ())
  }

  def sendEnvelope(
    attributeId: UUID,
    tenant: PersistentTenant,
    verifier: PersistentTenantVerifier,
    mailTemplate: MailTemplate
  ): Future[Unit] = {
    val envelopeId: UUID = UUIDSupplier.get()

    def createBody(attributeName: String, providerName: String): String = {
      val ifRevoke = if (verifier.renewal == PersistentVerificationRenewal.REVOKE_ON_EXPIRATION) {
        (
          "L'attributo ti verrà revocato e questo potrebbe avere impatti sullo stato di alcune tue richieste di fruizione.",
          "L'attributo ti è stato revocato e questo potrebbe avere impatti sullo stato di alcune tue richieste di fruizione."
        )
      } else ("", "")

      val ifRenewal = if (verifier.renewal == PersistentVerificationRenewal.AUTOMATIC_RENEWAL) {
        (
          "Per scelta del fruitore, l'attributo ti verrà rinnovato automaticamente; non ci sono quindi impatti sulle tue richieste di fruizione.",
          "Per scelta del fruitore, l'attributo è stato rinnovato automaticamente; non ci sono quindi impatti sulle tue richieste di fruizione."
        )
      } else ("", "")

      mailTemplate.body.interpolate(
        Map(
          "ifRevokeExpiration"  -> ifRevoke._1,
          "ifRenewalExpiration" -> ifRenewal._1,
          "ifRevokeExpired"     -> ifRevoke._2,
          "ifRenewalExpired"    -> ifRenewal._2,
          "attributeName"       -> attributeName,
          "providerName"        -> providerName
        )
      )
    }

    val subject: String = mailTemplate.subject

    val envelope: Future[InteropEnvelope] = for {
      tenantSelfcareId <- tenant.selfcareId.toFuture(SelfcareIdNotFound(tenant.id))
      provider         <- tenantProcess.getTenant(verifier.id)
      tenantSelfcare   <- partyProcessService.getInstitution(tenantSelfcareId)
      attribute        <- attributeRegistryProcess.getAttributeById(attributeId)
    } yield InteropEnvelope(
      id = envelopeId,
      recipients = List(tenantSelfcare.digitalAddress),
      subject = subject,
      body = createBody(attribute.name, provider.name),
      attachments = List.empty
    )

    envelope.flatMap(queueService.send[InteropEnvelope]).map(_ => ())
  }
}
