package it.pagopa.interop.tenantsattributeschecker.util

import it.pagopa.interop.certifiedMailSender.InteropEnvelope
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
import it.pagopa.interop.tenantsattributeschecker.util.errors._

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
          _ <- agreementProcess.computeAgreementsByAttribute(tenant.id, attribute.id)
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
      recipient: String,
      attributeName: String,
      producerName: String,
      consumerName: String
    ): InteropEnvelope =
      InteropEnvelope(
        id = uuidSupplier.get(),
        recipients = List(recipient),
        subject = template.subject,
        body = createBody(template, attributeName, producerName, consumerName),
        attachments = List.empty
      )

    for {
      producer           <- tenantProcess.getTenant(verifier.id)
      producerSelfcareId <- producer.selfcareId.toFuture(SelfcareIdNotFound(producer.id))
      consumerSelfcareId <- consumer.selfcareId.toFuture(SelfcareIdNotFound(consumer.id))
      producerSelfcare   <- selfcareClientService.getInstitution(producerSelfcareId)
      consumerSelfcare   <- selfcareClientService.getInstitution(consumerSelfcareId)
      attribute          <- attributeRegistryProcess.getAttributeById(attributeId)
      consumerEnvelope = createEnvelope(
        consumerMailTemplate,
        consumerSelfcare.digitalAddress,
        attribute.name,
        producer.name,
        consumer.name
      )
      producerEnvelope = createEnvelope(
        producerMailTemplate,
        producerSelfcare.digitalAddress,
        attribute.name,
        producer.name,
        consumer.name
      )
      _ <- queueService.send[InteropEnvelope](consumerEnvelope)
      _ <- queueService.send[InteropEnvelope](producerEnvelope)
    } yield ()
  }
}
