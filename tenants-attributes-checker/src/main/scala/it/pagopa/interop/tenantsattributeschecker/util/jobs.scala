package it.pagopa.interop.tenantsattributeschecker.util

import it.pagopa.interop.certifiedMailSender.InteropEnvelope
import it.pagopa.interop.commons.utils.TypeConversions.{OptionOps, _}
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
  context,
  selfcareV2ApiKey,
  selfcareV2URL
}
import it.pagopa.interop.tenantsattributeschecker.service._
import it.pagopa.interop.tenantsattributeschecker.service.impl._
import it.pagopa.interop.tenantsattributeschecker.util.errors._

import java.util.UUID
import scala.concurrent.Future

object jobs {

  private val agreementProcess: AgreementProcessService                 = AgreementProcessServiceImpl(blockingEc)
  private val tenantProcess: TenantProcessService                       = TenantProcessServiceImpl(blockingEc)
  private val attributeRegistryProcess: AttributeRegistryProcessService =
    AttributeRegistryProcessServiceImpl(blockingEc)
  private val queueService: QueueService                                = new QueueServiceImpl(certifiedMailQueueName)
  private val selfcareClientService: SelfcareClientService              =
    new SelfcareClientServiceImpl(selfcareV2URL, selfcareV2ApiKey)
  private val (consumerExpiredTemplate, producerExpiredTemplate): (MailTemplate, MailTemplate)   = MailTemplate.expired
  private val (consumerExpiringTemplate, producerExpiringTemplate): (MailTemplate, MailTemplate) =
    MailTemplate.expiring

  def applyStrategyOnExpiredAttributes(tenants: List[PersistentTenant]): Future[Unit] = {

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
              _ <- sendEnvelope(attribute.id, tenant, verifiedBy, consumerExpiredTemplate, producerExpiredTemplate)
            } yield ()
          }
        }
      }
      .map(_ => ())
  }

  def applyStrategyOnExpiringAttributes(tenants: List[PersistentTenant]): Future[Unit] = {
    Future
      .traverse(tenants) { tenant =>
        Future.traverse(tenant.attributes.collect { case v: PersistentVerifiedAttribute => v }) { attribute =>
          Future.traverse(attribute.verifiedBy) { verifiedBy =>
            sendEnvelope(attribute.id, tenant, verifiedBy, consumerExpiringTemplate, producerExpiringTemplate)
          }
        }
      }
      .map(_ => ())
  }

  def sendEnvelope(
    attributeId: UUID,
    consumer: PersistentTenant,
    verifier: PersistentTenantVerifier,
    consumerMailTemplate: MailTemplate,
    producerMailTemplate: MailTemplate
  ): Future[Unit] = {

    case class Message(expiration: String, expired: String)

    def createRevokeMessage(expiration: String, expired: String): Message =
      if (verifier.renewal == PersistentVerificationRenewal.REVOKE_ON_EXPIRATION) Message(expiration, expired)
      else Message("", "")

    def createRenewalMessage(expiration: String, expired: String): Message =
      if (verifier.renewal == PersistentVerificationRenewal.AUTOMATIC_RENEWAL) Message(expiration, expired)
      else Message("", "")

    def createBody(
      template: MailTemplate,
      revokeMsg: Message,
      renewalMsg: Message,
      attributeName: String,
      producerName: String,
      consumerName: String
    ): String =
      template.body.interpolate(
        Map(
          "ifRevokeExpiration"  -> revokeMsg.expiration,
          "ifRenewalExpiration" -> renewalMsg.expiration,
          "ifRevokeExpired"     -> revokeMsg.expired,
          "ifRenewalExpired"    -> renewalMsg.expired,
          "attributeName"       -> attributeName,
          "producerName"        -> producerName,
          "consumerName"        -> consumerName
        )
      )

    def createProducerBody(attributeName: String, producerName: String, consumerName: String): String = {
      val ifRevoke = createRevokeMessage(
        "L'attributo sarà revocato e questo potrebbe avere impatti sullo stato di alcune sue richieste di fruizione.",
        "L'attributo è stato revocato al fruitore e questo potrebbe avere impatti sullo stato di alcune sue richieste di fruizione."
      )

      val ifRenewal = createRenewalMessage(
        "Per tua scelta, l'attributo verrà rinnovato automaticamente; non ci sono quindi impatti sulle sue richieste di fruizione.",
        "Per tua scelta, l'attributo è stato rinnovato automaticamente; non ci sono quindi impatti sulle sue richieste di fruizione."
      )

      createBody(producerMailTemplate, ifRevoke, ifRenewal, attributeName, producerName, consumerName)
    }

    def createConsumerBody(attributeName: String, producerName: String, consumerName: String): String = {
      val ifRevoke = createRevokeMessage(
        "L'attributo ti verrà revocato e questo potrebbe avere impatti sullo stato di alcune tue richieste di fruizione.",
        "L'attributo ti è stato revocato e questo potrebbe avere impatti sullo stato di alcune tue richieste di fruizione."
      )

      val ifRenewal = createRenewalMessage(
        "Per scelta del fruitore, l'attributo ti verrà rinnovato automaticamente; non ci sono quindi impatti sulle tue richieste di fruizione.",
        "Per scelta del fruitore, l'attributo è stato rinnovato automaticamente; non ci sono quindi impatti sulle tue richieste di fruizione."
      )

      createBody(consumerMailTemplate, ifRevoke, ifRenewal, attributeName, producerName, consumerName)
    }

    val envelope: Future[(InteropEnvelope, InteropEnvelope)] = for {
      producer           <- tenantProcess.getTenant(verifier.id)
      producerSelfcareId <- producer.selfcareId.toFuture(SelfcareIdNotFound(producer.id))
      consumerSelfcareId <- consumer.selfcareId.toFuture(SelfcareIdNotFound(consumer.id))
      producerSelfcare   <- selfcareClientService.getInstitution(producerSelfcareId)
      consumerSelfcare   <- selfcareClientService.getInstitution(consumerSelfcareId)
      attribute          <- attributeRegistryProcess.getAttributeById(attributeId)
    } yield (
      InteropEnvelope(
        id = UUIDSupplier.get(),
        recipients = List(consumerSelfcare.digitalAddress),
        subject = consumerMailTemplate.subject,
        body = createConsumerBody(attribute.name, producer.name, consumer.name),
        attachments = List.empty
      ),
      InteropEnvelope(
        id = UUIDSupplier.get(),
        recipients = List(producerSelfcare.digitalAddress),
        subject = producerMailTemplate.subject,
        body = createProducerBody(attribute.name, producer.name, consumer.name),
        attachments = List.empty
      )
    )

    envelope.flatMap { case (consumerEnvelope, producerEnvelope) =>
      for {
        _ <- queueService.send[InteropEnvelope](consumerEnvelope)
        _ <- queueService.send[InteropEnvelope](producerEnvelope)
      } yield ()
    }
  }
}
