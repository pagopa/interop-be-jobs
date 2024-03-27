package it.pagopa.interop.tenantscertifiedattributesupdater

import it.pagopa.interop.commons.utils.Digester
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
  test("Update the tenant with attributes if the tenant exists with non-empty selfcareId") {
    val origin1                                   = "ORIGIN_1"
    val origin2                                   = "ORIGIN_2"
    val originId1                                 = "001"
    val originId2                                 = "002"
    val attributeCode1                            = "CAT1"
    val attributeCode2                            = "CAT2"
    val kind                                      = "KIND"
    val kindSha256                                = Digester.toSha256(kind.getBytes())
    val institutions: List[Institution]           =
      List(
        institution(origin1, originId1, attributeCode1, kind),
        institution(origin1, originId1, attributeCode2, kind),
        institution(origin2, originId1, attributeCode2, kind),
        institution(origin1, originId2, attributeCode2, kind)
      )
    val tenants: List[PersistentTenant]           =
      List(persistentTenant(origin1, originId1))
    val attributesIndex: Map[UUID, AttributeInfo] = Map.empty

    val result = createAction(institutions, tenants, attributesIndex)

    val expectedActivations = List(
      InternalTenantSeed(
        externalId = ExternalId(origin1, originId1),
        certifiedAttributes = List(
          InternalAttributeSeed(origin1, kindSha256),
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
    val kind           = "KIND"
    val kindSha256     = Digester.toSha256(kind.getBytes())

    val institutions: List[Institution]           =
      List(institution(origin, originId, attributeCode1, kind), institution(origin, originId, attributeCode2, kind))
    val tenants: List[PersistentTenant]           = List(persistentTenant(origin, originId))
    val attributesIndex: Map[UUID, AttributeInfo] = Map.empty

    val result = createAction(institutions, tenants, attributesIndex)

    val expectedActivations = List(
      InternalTenantSeed(
        externalId = ExternalId(origin, originId),
        certifiedAttributes = List(
          InternalAttributeSeed(origin, kindSha256),
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
    val kind                  = "KIND"
    val kindSha256            = Digester.toSha256(kind.getBytes())

    val institutions: List[Institution]           =
      List(
        institution(origin, originId, existingAttributeCode, kind),
        institution(origin, originId, attributeCode2, kind),
        institution(origin, originId, attributeCode3, kind)
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
          InternalAttributeSeed(origin, kindSha256),
          InternalAttributeSeed(origin, attributeCode3)
        ),
        name = "test_name"
      )
    )
    val expectedRevocations = Map.empty[PersistentExternalId, List[AttributeInfo]]

    assertEquals(result.activations.map(_.name), expectedActivations.map(_.name))
    assertEquals(result.activations.map(_.externalId), expectedActivations.map(_.externalId))
    assertEquals(
      result.activations.map(_.certifiedAttributes.toSet),
      expectedActivations.map(_.certifiedAttributes.toSet)
    )
    assertEquals(result.revocations, expectedRevocations)
  }

  test("Re-assign a previously revoked attribute") {
    val attributeId   = UUID.randomUUID()
    val origin        = "ORIGIN"
    val originId      = "OO1"
    val attributeCode = "CAT1"
    val kind          = "KIND"
    val kindSha256    = Digester.toSha256(kind.getBytes())

    val institutions: List[Institution]           = List(institution(origin, originId, attributeCode, kind))
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
        certifiedAttributes = List(
          InternalAttributeSeed(origin, kindSha256),
          InternalAttributeSeed(origin, attributeCode),
          InternalAttributeSeed(origin, originId)
        ),
        name = defaultName
      )
    )
    val expectedRevocations = Map.empty[PersistentExternalId, List[AttributeInfo]]

    assertEquals(result.activations, expectedActivations)
    assertEquals(result.revocations, expectedRevocations)
  }

  test("Revoke attributes if assigned to the Tenant") {
    val origin                 = "IPA"
    val existingAttributeId1   = UUID.randomUUID()
    val originId               = "001"
    val existingAttributeId2   = UUID.randomUUID()
    val existingAttributeCode2 = "CAT1"
    val existingAttributeId3   = UUID.randomUUID()
    val existingAttributeCode3 = "CAT2"
    val existingAttributeId4   = UUID.randomUUID()
    val kind                   = "KIND"
    val existingAttributeCode4 = Digester.toSha256(kind.getBytes)

    val institutions: List[Institution]           = List(institution(origin, originId, existingAttributeCode2, kind))
    val tenants: List[PersistentTenant]           = List(
      persistentTenant(
        origin,
        originId,
        attributes = List(
          PersistentCertifiedAttribute(id = existingAttributeId1, assignmentTimestamp = timestamp, None),
          PersistentCertifiedAttribute(id = existingAttributeId2, assignmentTimestamp = timestamp, None),
          PersistentCertifiedAttribute(id = existingAttributeId3, assignmentTimestamp = timestamp, None),
          PersistentCertifiedAttribute(id = existingAttributeId4, assignmentTimestamp = timestamp, None)
        )
      )
    )
    val attributesIndex: Map[UUID, AttributeInfo] =
      Map(
        existingAttributeId1 -> AttributeInfo(origin, originId, None),
        existingAttributeId2 -> AttributeInfo(origin, existingAttributeCode2, None),
        existingAttributeId3 -> AttributeInfo(origin, existingAttributeCode3, None),
        existingAttributeId4 -> AttributeInfo(origin, existingAttributeCode4, None)
      )

    val result = createAction(institutions, tenants, attributesIndex)

    val expectedActivations = Nil
    val expectedRevocations =
      Map(PersistentExternalId(origin, originId) -> List(AttributeInfo(origin, existingAttributeCode3, None)))

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
    val originFromRegistry     = "IPA"
    val originIdFromRegistry   = "001"
    val existingAttributeId1   = UUID.randomUUID()
    val existingAttributeCode1 = "CAT1"
    val originNotFromRegistry  = "AGID"
    val existingAttributeId2   = UUID.randomUUID()
    val existingAttributeCode2 = "SDG"

    val institutions: List[Institution]           = List.empty
    val tenants: List[PersistentTenant]           = List(
      persistentTenant(
        originFromRegistry,
        originIdFromRegistry,
        attributes = List(
          PersistentCertifiedAttribute(id = existingAttributeId1, assignmentTimestamp = timestamp, None),
          PersistentCertifiedAttribute(id = existingAttributeId2, assignmentTimestamp = timestamp, None)
        )
      )
    )
    val attributesIndex: Map[UUID, AttributeInfo] =
      Map(
        existingAttributeId1 -> AttributeInfo(originFromRegistry, existingAttributeCode1, None),
        existingAttributeId2 -> AttributeInfo(originNotFromRegistry, existingAttributeCode2, None)
      )

    val result = createAction(institutions, tenants, attributesIndex)

    val expectedActivations = Nil
    val expectedRevocations = Map.empty[PersistentExternalId, List[AttributeInfo]]

    assertEquals(result.activations, expectedActivations)
    assertEquals(result.revocations, expectedRevocations)
  }

}
