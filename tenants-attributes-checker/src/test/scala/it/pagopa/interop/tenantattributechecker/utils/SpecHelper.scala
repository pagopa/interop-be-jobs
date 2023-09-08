package it.pagopa.interop.tenantattributechecker.utils

import it.pagopa.interop.attributeregistryprocess.client.model.Attribute
import it.pagopa.interop.commons.mail.InteropEnvelope
import it.pagopa.interop.commons.utils._
import it.pagopa.interop.selfcare.v2.client.model.Institution
import it.pagopa.interop.tenantmanagement.model.tenant.PersistentTenant
import it.pagopa.interop.tenantprocess.client.model.Tenant
import it.pagopa.interop.tenantsattributeschecker.service._
import org.scalamock.scalatest.MockFactory
import spray.json.JsonWriter

import java.util.UUID
import scala.concurrent.Future

trait SpecHelper extends MockFactory with SpecData {

  val bearerToken          = "token"
  val organizationId: UUID = UUID.randomUUID()

  val m2mContext: Seq[(String, String)]      =
    Seq("bearer" -> bearerToken, USER_ROLES -> "m2m", ORGANIZATION_ID_CLAIM -> organizationId.toString)
  val internalContext: Seq[(String, String)] =
    Seq("bearer" -> bearerToken, USER_ROLES -> "internal")
  val adminContext: Seq[(String, String)]    =
    Seq("bearer" -> bearerToken, USER_ROLES -> "admin", ORGANIZATION_ID_CLAIM -> organizationId.toString)

  val mockTenantProcess: TenantProcessService                       = mock[TenantProcessService]
  val mockAgreementProcess: AgreementProcessService                 = mock[AgreementProcessService]
  val mockSelfcareClient: SelfcareClientService                     = mock[SelfcareClientService]
  val mockAttributeRegistryProcess: AttributeRegistryProcessService = mock[AttributeRegistryProcessService]
  val mockQueueService: QueueService                                = mock[QueueService]

  def setupMocks(
    attribute: Attribute,
    producer: Tenant,
    consumer: PersistentTenant,
    producerSelfcare: Institution,
    consumerSelfcare: Institution,
    producerEnvelope: InteropEnvelope,
    consumerEnvelope: InteropEnvelope
  ): Unit = {

    (mockTenantProcess
      .getTenant(_: UUID)(_: Seq[(String, String)]))
      .expects(producer.id, *)
      .returns(Future.successful(producer))
      .once(): Unit

    (mockSelfcareClient
      .getInstitution(_: String)(_: Seq[(String, String)]))
      .expects(producer.selfcareId.getOrElse[String](""), *)
      .returns(Future.successful(producerSelfcare))
      .once(): Unit

    (mockSelfcareClient
      .getInstitution(_: String)(_: Seq[(String, String)]))
      .expects(consumer.selfcareId.getOrElse[String](""), *)
      .returns(Future.successful(consumerSelfcare))
      .once(): Unit

    (mockAttributeRegistryProcess
      .getAttributeById(_: UUID)(_: Seq[(String, String)]))
      .expects(attribute.id, *)
      .returns(Future.successful(attribute))
      .once(): Unit

    (mockQueueService
      .send[InteropEnvelope](_: InteropEnvelope)(_: JsonWriter[InteropEnvelope]))
      .expects(consumerEnvelope, *)
      .returns(Future.successful("sent"))
      .once(): Unit

    (mockQueueService
      .send[InteropEnvelope](_: InteropEnvelope)(_: JsonWriter[InteropEnvelope]))
      .expects(producerEnvelope, *)
      .returns(Future.successful("sent"))
      .once(): Unit
  }

}
