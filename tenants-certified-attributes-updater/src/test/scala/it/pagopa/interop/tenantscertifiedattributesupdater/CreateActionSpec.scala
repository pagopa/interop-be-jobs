//package it.pagopa.interop.tenantscertifiedattributesupdater
//
//import it.pagopa.interop.partyregistryproxy.client.model.Institution
//import it.pagopa.interop.tenantmanagement.model.tenant.{
//  PersistentCertifiedAttribute,
//  PersistentExternalId,
//  PersistentTenant
//}
//import it.pagopa.interop.tenantprocess.client.model.{ExternalId, InternalAttributeSeed, InternalTenantSeed}
//import it.pagopa.interop.tenantscertifiedattributesupdater.SpecHelper._
//import it.pagopa.interop.tenantscertifiedattributesupdater.util.{AttributeInfo, createAction}
//import munit.FunSuite
//
//import java.util.UUID
//
//class CreateActionSpec extends FunSuite {
//  test("Create new Tenant with attributes if Tenant does not exist") {
//    val attributeOrigin1 = "IPA"
//    val attributeCode1   = "CAT1"
//    val attributeOrigin2 = "IPA"
//    val attributeCode2   = "CAT2"
//
//    val institutions: List[Institution]           =
//      List(
//        institution("IPA", "001", attributeCode1),
//        institution("IPA", "001", attributeCode2),
//        institution("IPA", "002", attributeCode2)
//      )
//    val tenants: List[PersistentTenant]           = List(persistentTenant("ORIG1", "VAL1"))
//    val attributesIndex: Map[UUID, AttributeInfo] = Map.empty
//
//    val result = createAction(institutions, tenants, attributesIndex)
//
//    val expectedActivations = Set(
//      InternalTenantSeed(
//        externalId = ExternalId("IPA", "001"),
//        certifiedAttributes = List(
//          InternalAttributeSeed(attributeOrigin1, attributeCode1),
//          InternalAttributeSeed(attributeOrigin2, attributeCode2)
//        )
//      ),
//      InternalTenantSeed(
//        externalId = ExternalId("IPA", "002"),
//        certifiedAttributes = List(InternalAttributeSeed(attributeOrigin2, attributeCode2))
//      )
//    )
//    val expectedRevocations = Map.empty[PersistentExternalId, List[AttributeInfo]]
//
//    assertEquals(result.activations.toSet, expectedActivations)
//    assertEquals(result.revocations, expectedRevocations)
//  }
//
//  test("Assign attributes to existing Tenant") {
//    val attributeOrigin1 = "IPA"
//    val attributeCode1   = "CAT1"
//    val attributeOrigin2 = "IPA"
//    val attributeCode2   = "CAT2"
//
//    val institutions: List[Institution]           =
//      List(institution("IPA", "001", attributeCode1), institution("IPA", "001", attributeCode2))
//    val tenants: List[PersistentTenant]           = List(persistentTenant("IPA", "001"))
//    val attributesIndex: Map[UUID, AttributeInfo] = Map.empty
//
//    val result = createAction(institutions, tenants, attributesIndex)
//
//    val expectedActivations = List(
//      InternalTenantSeed(
//        externalId = ExternalId("IPA", "001"),
//        certifiedAttributes = List(
//          InternalAttributeSeed(attributeOrigin1, attributeCode1),
//          InternalAttributeSeed(attributeOrigin2, attributeCode2)
//        )
//      )
//    )
//    val expectedRevocations = Map.empty[PersistentExternalId, List[AttributeInfo]]
//
//    assertEquals(result.activations, expectedActivations)
//    assertEquals(result.revocations, expectedRevocations)
//  }
//
//  test("Assign only new attributes to existing Tenant") {
//    val existingAttributeId     = UUID.randomUUID()
//    val existingAttributeOrigin = "IPA"
//    val existingAttributeCode   = "CAT1"
//
//    val attributeOrigin2 = "IPA"
//    val attributeCode2   = "CAT2"
//    val attributeOrigin3 = "IPA"
//    val attributeCode3   = "CAT3"
//
//    val institutions: List[Institution]           =
//      List(
//        institution("IPA", "001", existingAttributeCode),
//        institution("IPA", "001", attributeCode2),
//        institution("IPA", "001", attributeCode3)
//      )
//    val tenants: List[PersistentTenant]           = List(
//      persistentTenant(
//        "IPA",
//        "001",
//        attributes = List(PersistentCertifiedAttribute(id = existingAttributeId, assignmentTimestamp = timestamp, None))
//      )
//    )
//    val attributesIndex: Map[UUID, AttributeInfo] =
//      Map(existingAttributeId -> AttributeInfo(existingAttributeOrigin, existingAttributeCode, None))
//
//    val result = createAction(institutions, tenants, attributesIndex)
//
//    val expectedActivations = List(
//      InternalTenantSeed(
//        externalId = ExternalId("IPA", "001"),
//        certifiedAttributes = List(
//          InternalAttributeSeed(attributeOrigin2, attributeCode2),
//          InternalAttributeSeed(attributeOrigin3, attributeCode3)
//        )
//      )
//    )
//    val expectedRevocations = Map.empty[PersistentExternalId, List[AttributeInfo]]
//
//    assertEquals(result.activations, expectedActivations)
//    assertEquals(result.revocations, expectedRevocations)
//  }
//
//  test("Re-assign a previously revoked attribute") {
//    val attributeId     = UUID.randomUUID()
//    val attributeOrigin = "IPA"
//    val attributeCode   = "CAT1"
//
//    val institutions: List[Institution]           = List(institution("IPA", "001", attributeCode))
//    val tenants: List[PersistentTenant]           = List(
//      persistentTenant(
//        "IPA",
//        "001",
//        attributes = List(
//          PersistentCertifiedAttribute(
//            id = attributeId,
//            assignmentTimestamp = timestamp,
//            revocationTimestamp = Some(timestamp)
//          )
//        )
//      )
//    )
//    val attributesIndex: Map[UUID, AttributeInfo] =
//      Map(attributeId -> AttributeInfo(attributeOrigin, attributeCode, None))
//
//    val result = createAction(institutions, tenants, attributesIndex)
//
//    val expectedActivations = List(
//      InternalTenantSeed(
//        externalId = ExternalId("IPA", "001"),
//        certifiedAttributes = List(InternalAttributeSeed(attributeOrigin, attributeCode))
//      )
//    )
//    val expectedRevocations = Map.empty[PersistentExternalId, List[AttributeInfo]]
//
//    assertEquals(result.activations, expectedActivations)
//    assertEquals(result.revocations, expectedRevocations)
//  }
//
//  test("Revoke attributes if assigned to the Tenant") {
//    val existingAttributeId1     = UUID.randomUUID()
//    val existingAttributeOrigin1 = "IPA"
//    val existingAttributeCode1   = "CAT1"
//    val existingAttributeId2     = UUID.randomUUID()
//    val existingAttributeOrigin2 = "IPA"
//    val existingAttributeCode2   = "CAT2"
//
//    val institutions: List[Institution]           = List(institution("IPA", "001", existingAttributeCode2))
//    val tenants: List[PersistentTenant]           = List(
//      persistentTenant(
//        "IPA",
//        "001",
//        attributes = List(
//          PersistentCertifiedAttribute(id = existingAttributeId1, assignmentTimestamp = timestamp, None),
//          PersistentCertifiedAttribute(id = existingAttributeId2, assignmentTimestamp = timestamp, None)
//        )
//      )
//    )
//    val attributesIndex: Map[UUID, AttributeInfo] =
//      Map(
//        existingAttributeId1 -> AttributeInfo(existingAttributeOrigin1, existingAttributeCode1, None),
//        existingAttributeId2 -> AttributeInfo(existingAttributeOrigin2, existingAttributeCode2, None)
//      )
//
//    val result = createAction(institutions, tenants, attributesIndex)
//
//    val expectedActivations = Nil
//    val expectedRevocations = Map(
//      PersistentExternalId("IPA", "001") -> List(AttributeInfo(existingAttributeOrigin1, existingAttributeCode1, None))
//    )
//
//    assertEquals(result.activations, expectedActivations)
//    assertEquals(result.revocations, expectedRevocations)
//  }
//
//  test("Keep an attribute revoked if still missing from the Certifier") {
//    val attributeId     = UUID.randomUUID()
//    val attributeOrigin = "IPA"
//    val attributeCode   = "CAT1"
//
//    val institutions: List[Institution]           = Nil
//    val tenants: List[PersistentTenant]           = List(
//      persistentTenant(
//        "IPA",
//        "001",
//        attributes = List(
//          PersistentCertifiedAttribute(
//            id = attributeId,
//            assignmentTimestamp = timestamp,
//            revocationTimestamp = Some(timestamp)
//          )
//        )
//      )
//    )
//    val attributesIndex: Map[UUID, AttributeInfo] =
//      Map(attributeId -> AttributeInfo(attributeOrigin, attributeCode, None))
//
//    val result = createAction(institutions, tenants, attributesIndex)
//
//    val expectedActivations = Nil
//    val expectedRevocations =
//      Map(PersistentExternalId("IPA", "001") -> List(AttributeInfo(attributeOrigin, attributeCode, Some(timestamp))))
//
//    assertEquals(result.activations, expectedActivations)
//    assertEquals(result.revocations, expectedRevocations)
//  }
//
//}
