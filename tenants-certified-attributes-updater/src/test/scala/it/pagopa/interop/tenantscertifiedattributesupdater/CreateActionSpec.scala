package it.pagopa.interop.tenantscertifiedattributesupdater

import it.pagopa.interop.partyregistryproxy.client.model.Institution
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

class CreateActionSpec extends FunSuite {
  test("Create new Tenant with attributes if Tenant does not exist") {
    val origin1        = "ORIGIN_1"
    val origin2        = "ORIGIN_2"
    val originId1      = "001"
    val originId2      = "002"
    val attributeCode1 = "CAT1"
    val attributeCode2 = "CAT2"

    val institutions: List[Institution]           =
      List(
        institution(origin1, originId1, attributeCode1),
        institution(origin1, originId1, attributeCode2),
        institution(origin2, originId1, attributeCode2),
        institution(origin1, originId2, attributeCode2)
      )
    val tenants: List[PersistentTenant]           = List(persistentTenant("ORIG1", "VAL1"))
    val attributesIndex: Map[UUID, AttributeInfo] = Map.empty

    val result = createAction(institutions, tenants, attributesIndex)

    val expectedActivations = List(
      InternalTenantSeed(
        externalId = ExternalId(origin2, originId1),
        certifiedAttributes =
          List(InternalAttributeSeed(origin2, attributeCode2), InternalAttributeSeed(origin2, originId1)),
        name = defaultName
      ),
      InternalTenantSeed(
        externalId = ExternalId(origin1, originId2),
        certifiedAttributes =
          List(InternalAttributeSeed(origin1, attributeCode2), InternalAttributeSeed(origin1, originId2)),
        name = defaultName
      ),
      InternalTenantSeed(
        externalId = ExternalId(origin1, originId1),
        certifiedAttributes = List(
          InternalAttributeSeed(origin1, attributeCode1),
          InternalAttributeSeed(origin1, originId1),
          InternalAttributeSeed(origin1, attributeCode2)
        ),
        name = defaultName
      )
    )
    val expectedRevocations = Map.empty[PersistentExternalId, List[AttributeInfo]]

    assertEquals(result.activations, expectedActivations)
    assertEquals(result.revocations, expectedRevocations)
  }

  test("Assign attributes to existing Tenant") {
    val origin         = "ORIGIN"
    val originId       = "001"
    val attributeCode1 = "CAT1"
    val attributeCode2 = "CAT2"

    val institutions: List[Institution]           =
      List(institution(origin, originId, attributeCode1), institution(origin, originId, attributeCode2))
    val tenants: List[PersistentTenant]           = List(persistentTenant(origin, originId))
    val attributesIndex: Map[UUID, AttributeInfo] = Map.empty

    val result = createAction(institutions, tenants, attributesIndex)

    val expectedActivations = List(
      InternalTenantSeed(
        externalId = ExternalId(origin, originId),
        certifiedAttributes = List(
          InternalAttributeSeed(origin, attributeCode1),
          InternalAttributeSeed(origin, originId),
          InternalAttributeSeed(origin, attributeCode2)
        ),
        name = defaultName
      )
    )
    val expectedRevocations = Map.empty[PersistentExternalId, List[AttributeInfo]]

    assertEquals(result.activations, expectedActivations)
    assertEquals(result.revocations, expectedRevocations)
  }

  test("Assign only new attributes to existing Tenant") {
    val origin                = "ORIGIN"
    val originId              = "001"
    val existingAttributeId   = UUID.randomUUID()
    val existingAttributeCode = "CAT1"
    val attributeCode2        = "CAT2"
    val attributeCode3        = "CAT3"

    val institutions: List[Institution]           =
      List(
        institution(origin, originId, existingAttributeCode),
        institution(origin, originId, attributeCode2),
        institution(origin, originId, attributeCode3)
      )
    val tenants: List[PersistentTenant]           = List(
      persistentTenant(
        origin,
        originId,
        attributes = List(PersistentCertifiedAttribute(id = existingAttributeId, assignmentTimestamp = timestamp, None))
      )
    )
    val attributesIndex: Map[UUID, AttributeInfo] =
      Map(existingAttributeId -> AttributeInfo(origin, existingAttributeCode, None))

    val result = createAction(institutions, tenants, attributesIndex)

    val expectedActivations = List(
      InternalTenantSeed(
        externalId = ExternalId(origin, originId),
        certifiedAttributes = List(
          InternalAttributeSeed(origin, attributeCode2),
          InternalAttributeSeed(origin, originId),
          InternalAttributeSeed(origin, attributeCode3)
        ),
        name = "test_name"
      )
    )
    val expectedRevocations = Map.empty[PersistentExternalId, List[AttributeInfo]]

    assertEquals(result.activations, expectedActivations)
    assertEquals(result.revocations, expectedRevocations)
  }

  test("Re-assign a previously revoked attribute") {
    val attributeId   = UUID.randomUUID()
    val origin        = "ORIGIN"
    val originId      = "OO1"
    val attributeCode = "CAT1"

    val institutions: List[Institution]           = List(institution(origin, originId, attributeCode))
    val tenants: List[PersistentTenant]           = List(
      persistentTenant(
        origin,
        originId,
        attributes = List(
          PersistentCertifiedAttribute(
            id = attributeId,
            assignmentTimestamp = timestamp,
            revocationTimestamp = Some(timestamp)
          )
        )
      )
    )
    val attributesIndex: Map[UUID, AttributeInfo] =
      Map(attributeId -> AttributeInfo(origin, attributeCode, None))

    val result = createAction(institutions, tenants, attributesIndex)

    val expectedActivations = List(
      InternalTenantSeed(
        externalId = ExternalId(origin, originId),
        certifiedAttributes =
          List(InternalAttributeSeed(origin, attributeCode), InternalAttributeSeed(origin, originId)),
        name = defaultName
      )
    )
    val expectedRevocations = Map.empty[PersistentExternalId, List[AttributeInfo]]

    assertEquals(result.activations, expectedActivations)
    assertEquals(result.revocations, expectedRevocations)
  }

  test("Revoke attributes if assigned to the Tenant") {
    val origin                 = "IPA"
    val originId               = "001"
    val existingAttributeId1   = UUID.randomUUID()
    val existingAttributeCode1 = "CAT1"
    val existingAttributeId2   = UUID.randomUUID()
    val existingAttributeCode2 = "CAT2"

    val institutions: List[Institution]           = List(institution(origin, originId, existingAttributeCode2))
    val tenants: List[PersistentTenant]           = List(
      persistentTenant(
        origin,
        originId,
        attributes = List(
          PersistentCertifiedAttribute(id = existingAttributeId1, assignmentTimestamp = timestamp, None),
          PersistentCertifiedAttribute(id = existingAttributeId2, assignmentTimestamp = timestamp, None)
        )
      )
    )
    val attributesIndex: Map[UUID, AttributeInfo] =
      Map(
        existingAttributeId1 -> AttributeInfo(origin, existingAttributeCode1, None),
        existingAttributeId2 -> AttributeInfo(origin, existingAttributeCode2, None)
      )

    val result = createAction(institutions, tenants, attributesIndex)

    val expectedActivations = Nil
    val expectedRevocations =
      Map(PersistentExternalId(origin, originId) -> List(AttributeInfo(origin, existingAttributeCode1, None)))

    assertEquals(result.activations, expectedActivations)
    assertEquals(result.revocations, expectedRevocations)
  }

  test("Keep an attribute revoked if still missing from the Certifier") {
    val attributeId     = UUID.randomUUID()
    val attributeOrigin = "IPA"
    val attributeCode   = "CAT1"

    val institutions: List[Institution]           = Nil
    val tenants: List[PersistentTenant]           = List(
      persistentTenant(
        "IPA",
        "001",
        attributes = List(
          PersistentCertifiedAttribute(
            id = attributeId,
            assignmentTimestamp = timestamp,
            revocationTimestamp = Some(timestamp)
          )
        )
      )
    )
    val attributesIndex: Map[UUID, AttributeInfo] =
      Map(attributeId -> AttributeInfo(attributeOrigin, attributeCode, None))

    val result = createAction(institutions, tenants, attributesIndex)

    val expectedActivations = Nil
    val expectedRevocations = Map.empty[PersistentExternalId, List[AttributeInfo]]

    assertEquals(result.activations, expectedActivations)
    assertEquals(result.revocations, expectedRevocations)
  }

  test("revoke only the attributes whose origin is also present in the register of institution") {
    val admittedOrigin         = "IPA"
    val admittedOriginId       = "001"
    val existingAttributeId1   = UUID.randomUUID()
    val existingAttributeCode1 = "CAT1"
    val forbiddenOrigin        = "AGID"
    val existingAttributeId2   = UUID.randomUUID()
    val existingAttributeCode2 = "SDG"

    val institutions: List[Institution]           = List.empty
    val tenants: List[PersistentTenant]           = List(
      persistentTenant(
        admittedOrigin,
        admittedOriginId,
        attributes = List(
          PersistentCertifiedAttribute(id = existingAttributeId1, assignmentTimestamp = timestamp, None),
          PersistentCertifiedAttribute(id = existingAttributeId2, assignmentTimestamp = timestamp, None)
        )
      )
    )
    val attributesIndex: Map[UUID, AttributeInfo] =
      Map(
        existingAttributeId1 -> AttributeInfo(admittedOrigin, existingAttributeCode1, None),
        existingAttributeId2 -> AttributeInfo(forbiddenOrigin, existingAttributeCode2, None)
      )

    val result = createAction(institutions, tenants, attributesIndex)

    val expectedActivations = Nil
    val expectedRevocations = Map.empty[PersistentExternalId, List[AttributeInfo]]

    assertEquals(result.activations, expectedActivations)
    assertEquals(result.revocations, expectedRevocations)
  }

}
