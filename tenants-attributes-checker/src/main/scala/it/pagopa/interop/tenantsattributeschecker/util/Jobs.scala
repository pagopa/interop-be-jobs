package it.pagopa.interop.tenantsattributeschecker.util

import it.pagopa.interop.agreementprocess.client.model.CompactTenant
import it.pagopa.interop.commons.mail.TextMail
import it.pagopa.interop.commons.utils.TypeConversions._
import it.pagopa.interop.commons.utils.service.UUIDSupplier
import it.pagopa.interop.tenantmanagement.model.tenant.{
  PersistentTenant,
  PersistentTenantVerifier,
  PersistentVerifiedAttribute
}
import it.pagopa.interop.tenantsattributeschecker.ApplicationConfiguration.{blockingEc, context}
import it.pagopa.interop.tenantsattributeschecker.service._
import it.pagopa.interop.tenantsattributeschecker.service.impl._
import it.pagopa.interop.tenantsattributeschecker.util.Adapters.DependencyTenantAttributeWrapper
import it.pagopa.interop.tenantsattributeschecker.util.errors._
import it.pagopa.interop.commons.mail.Mail

import javax.mail.internet.InternetAddress
import java.util.UUID
import scala.concurrent.Future

class Jobs(
  agreementProcess: AgreementProcessService,
  tenantProcess: TenantProcessService,
  attributeRegistryProcess: AttributeRegistryProcessService,
  queueService: QueueService,
  selfcareClientService: SelfcareClientService,
  uuidSupplier: UUIDSupplier
) {

  private val (consumerExpiredTemplate, producerExpiredTemplate): (MailTemplate, MailTemplate)   = MailTemplate.expired
  private val (consumerExpiringTemplate, producerExpiringTemplate): (MailTemplate, MailTemplate) =
    MailTemplate.expiring

  def applyStrategyOnExpiredAttributes(tenants: List[PersistentTenant]): Future[Unit] = {

    val data: List[(PersistentTenant, PersistentVerifiedAttribute, PersistentTenantVerifier)] = for {
      tenant     <- tenants
      attribute  <- tenant.attributes.collect { case v: PersistentVerifiedAttribute => v }
      verifiedBy <- attribute.verifiedBy
    } yield (tenant, attribute, verifiedBy)

    Future
      .traverseWithLatch(10)(data) { case (tenant, attribute, verifiedBy) =>
        for {
          _ <- agreementProcess.computeAgreementsByAttribute(
            attribute.id,
            CompactTenant(tenant.id, tenant.attributes.map(_.toAgreementApi))
          )
          _ <- sendEnvelope(attribute.id, tenant, verifiedBy, consumerExpiredTemplate, producerExpiredTemplate)
        } yield ()
      }
      .map(_ => ())
  }

  def applyStrategyOnExpiringAttributes(tenants: List[PersistentTenant]): Future[Unit] = {

    val data: List[(PersistentTenant, PersistentVerifiedAttribute, PersistentTenantVerifier)] = for {
      tenant     <- tenants
      attribute  <- tenant.attributes.collect { case v: PersistentVerifiedAttribute => v }
      verifiedBy <- attribute.verifiedBy
    } yield (tenant, attribute, verifiedBy)

    Future
      .traverseWithLatch(10)(data) { case (tenant, attribute, verifiedBy) =>
        sendEnvelope(attribute.id, tenant, verifiedBy, consumerExpiringTemplate, producerExpiringTemplate)
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

    def createBody(template: MailTemplate, attributeName: String, producerName: String, consumerName: String): String =
      template.body.interpolate(
        Map("attributeName" -> attributeName, "producerName" -> producerName, "consumerName" -> consumerName)
      )

    def createEnvelope(
      template: MailTemplate,
      recipients: Seq[InternetAddress],
      attributeName: String,
      producerName: String,
      consumerName: String
    ): TextMail =
      TextMail(
        id = uuidSupplier.get(),
        recipients = recipients,
        subject = template.subject,
        body = createBody(template, attributeName, producerName, consumerName),
        attachments = List.empty
      )

    for {
      producer              <- tenantProcess.getTenant(verifier.id)
      producerSelfcareId    <- producer.selfcareId.toFuture(SelfcareIdNotFound(producer.id))
      consumerSelfcareId    <- consumer.selfcareId.toFuture(SelfcareIdNotFound(consumer.id))
      producerSelfcare      <- selfcareClientService.getInstitution(producerSelfcareId)
      consumerSelfcare      <- selfcareClientService.getInstitution(consumerSelfcareId)
      attribute             <- attributeRegistryProcess.getAttributeById(attributeId)
      consumerDigitalAdress <- consumerSelfcare.digitalAddress.toFuture(
        SelfcareEntityNotFilled(consumerSelfcare.getClass().getName(), "digitalAddress")
      )
      producerDigitalAdress <- producerSelfcare.digitalAddress.toFuture(
        SelfcareEntityNotFilled(producerSelfcare.getClass().getName(), "digitalAddress")
      )
      consumerAddresses     <- Mail.addresses(consumerDigitalAdress).toFuture
      producerAddresses     <- Mail.addresses(producerDigitalAdress).toFuture
      consumerEnvelope = createEnvelope(
        consumerMailTemplate,
        consumerAddresses,
        attribute.name,
        producer.name,
        consumer.name
      )
      producerEnvelope = createEnvelope(
        producerMailTemplate,
        producerAddresses,
        attribute.name,
        producer.name,
        consumer.name
      )
      _ <- queueService.send[TextMail](consumerEnvelope)
      _ <- queueService.send[TextMail](producerEnvelope)
    } yield ()
  }
}
