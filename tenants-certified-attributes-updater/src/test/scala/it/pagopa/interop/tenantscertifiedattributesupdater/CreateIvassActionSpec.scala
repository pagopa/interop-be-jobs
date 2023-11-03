package it.pagopa.interop.tenantscertifiedattributesupdater

import it.pagopa.interop.tenantmanagement.model.tenant.{
  PersistentCertifiedAttribute,
  PersistentExternalId,
  PersistentTenant
}
import it.pagopa.interop.tenantprocess.client.model.{ExternalId, InternalAttributeSeed, InternalTenantSeed}
import it.pagopa.interop.tenantscertifiedattributesupdater.SpecHelper._
import it.pagopa.interop.tenantscertifiedattributesupdater.util.AttributeInfo
import it.pagopa.interop.tenantscertifiedattributesupdater.util.Utils._
import munit.FunSuite

import java.util.UUID

class CreateIvassActionSpec extends FunSuite {

  test("Assign attributes to existing Tenant") {
    val originIvass        = "IVASS"
    val otherOrigin        = "IPA"
    val originId           = "001"
    val attributeCodeIvass = "code"
    val attributeUUID      = UUID.randomUUID()

    val tenants: List[PersistentTenant]           =
      List(persistentTenant(originIvass, originId), persistentTenant(otherOrigin, originId))
    val attributesIndex: Map[UUID, AttributeInfo] =
      Map(attributeUUID -> AttributeInfo(originIvass, attributeCodeIvass, None))

    val result = createIvassAction(tenants, attributesIndex, attributeCodeIvass)

    val expectedActivations = List(
      InternalTenantSeed(
        externalId = ExternalId(originIvass, originId),
        certifiedAttributes = List(InternalAttributeSeed(originIvass, attributeCodeIvass)),
        name = defaultName
      )
    )
    val expectedRevocations = Map.empty[PersistentExternalId, List[AttributeInfo]]

    assertEquals(result.activations, expectedActivations)
    assertEquals(result.revocations, expectedRevocations)
  }

  test("Assign only new attributes to existing Tenant") {
    val originIvass        = "IVASS"
    val otherOrigin        = "IPA"
    val originId           = "001"
    val attributeCodeIvass = "Assicurazioni-code"
    val attributeUUID      = UUID.randomUUID()

    val tenants: List[PersistentTenant]           =
      List(
        persistentTenant(
          originIvass,
          originId,
          attributes = List(PersistentCertifiedAttribute(attributeUUID, timestamp, None))
        ),
        persistentTenant(otherOrigin, originId)
      )
    val attributesIndex: Map[UUID, AttributeInfo] =
      Map(attributeUUID -> AttributeInfo(originIvass, attributeCodeIvass, None))

    val result = createIvassAction(tenants, attributesIndex, attributeCodeIvass)

    val expectedActivations = Nil
    val expectedRevocations = Map.empty[PersistentExternalId, List[AttributeInfo]]

    assertEquals(result.activations, expectedActivations)
    assertEquals(result.revocations, expectedRevocations)
  }
}
