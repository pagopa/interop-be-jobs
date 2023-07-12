package it.pagopa.interop.attributesloader

import com.mongodb.client.model.Filters
import it.pagopa.interop.attributeregistrymanagement.model.persistence.{attribute => PersistentAttributeDependency}
import it.pagopa.interop.attributeregistryprocess.client.model.{Attribute, AttributeKind, AttributeSeed}
import it.pagopa.interop.commons.utils.Digester
import it.pagopa.interop.commons.utils.service.OffsetDateTimeSupplier
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers._
import org.scalatest.time.{Second, Span}
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

class AttributeRegistryApiServiceSpec extends AnyWordSpecLike with SpecHelper with ScalaFutures {

  private val testTimeout: Timeout = Timeout(Span(1, Second))

  "Attributes loading" must {
    "succeed with creation of all attributes retrieved from both Categories and Institutions if ReadModel is empty" in {

      mockGetCategories(Some(1), Some(50), result = categories)
      mockGetInstitutions(Some(1), Some(50), result = institutions)
      mockFind[PersistentAttributeDependency.PersistentAttribute](
        "attributes",
        Filters.empty(),
        0,
        50,
        result = Seq[PersistentAttributeDependency.PersistentAttribute]()
      )

      val expectedDelta: Set[AttributeSeed] = attributeSeeds.toSet

      expectedDelta.foreach(expectedAttributeSeed =>
        mockCreateAttribute(
          expectedAttributeSeed,
          Attribute(
            id = UUID.randomUUID(),
            code = expectedAttributeSeed.code,
            kind = expectedAttributeSeed.kind,
            description = expectedAttributeSeed.description,
            origin = expectedAttributeSeed.origin,
            name = expectedAttributeSeed.name,
            creationTime = OffsetDateTimeSupplier.get()
          )
        )
      )

      jobs.loadCertifiedAttributes().futureValue(testTimeout) shouldEqual ()
    }

    "succeed with creation of delta of attributes retrieved from both Categories and Institutions if ReadModel is not empty" in {

      mockGetCategories(Some(1), Some(50), result = categories)
      mockGetInstitutions(Some(1), Some(50), result = institutions)

      val attributesfromRM: Seq[PersistentAttributeDependency.PersistentAttribute] =
        Seq[PersistentAttributeDependency.PersistentAttribute](
          PersistentAttributeDependency.PersistentAttribute(
            id = UUID.randomUUID(),
            code = Some("YADA"),
            kind = PersistentAttributeDependency.Certified,
            description = "YADA",
            origin = Some("IPA"),
            name = "YADA",
            creationTime = OffsetDateTimeSupplier.get()
          ),
          PersistentAttributeDependency.PersistentAttribute(
            id = UUID.randomUUID(),
            code = Some("104532"),
            kind = PersistentAttributeDependency.Certified,
            description = "104532",
            origin = Some("IPA"),
            name = "104532",
            creationTime = OffsetDateTimeSupplier.get()
          )
        )

      mockFind[PersistentAttributeDependency.PersistentAttribute](
        "attributes",
        Filters.empty(),
        0,
        50,
        result = attributesfromRM
      )

      val expectedDelta: Set[AttributeSeed] = Set[AttributeSeed](
        AttributeSeed(
          code = Some("OPA"),
          kind = AttributeKind.CERTIFIED,
          description = "OPA",
          origin = Some("IPA"),
          name = "OPA"
        ),
        AttributeSeed(
          code = Some(Digester.toSha256(admittedAttributeKind.getBytes())),
          kind = AttributeKind.CERTIFIED,
          description = admittedAttributeKind,
          origin = Some("IPA"),
          name = admittedAttributeKind
        ),
        AttributeSeed(
          code = Some("205942"),
          kind = AttributeKind.CERTIFIED,
          description = "205942",
          origin = Some("IPA"),
          name = "205942"
        )
      )

      expectedDelta.foreach(expectedAttributeSeed =>
        mockCreateAttribute(
          expectedAttributeSeed,
          Attribute(
            id = UUID.randomUUID(),
            code = expectedAttributeSeed.code,
            kind = expectedAttributeSeed.kind,
            description = expectedAttributeSeed.description,
            origin = expectedAttributeSeed.origin,
            name = expectedAttributeSeed.name,
            creationTime = OffsetDateTimeSupplier.get()
          )
        )
      )

      jobs.loadCertifiedAttributes().futureValue(testTimeout) shouldEqual ()
    }

    "succeed if no attributes are created when ReadModel already contains them" in {

      mockGetCategories(Some(1), Some(50), result = categories)
      mockGetInstitutions(Some(1), Some(50), result = institutions)

      val attributesfromRM: Seq[PersistentAttributeDependency.PersistentAttribute] =
        Seq[PersistentAttributeDependency.PersistentAttribute](
          PersistentAttributeDependency.PersistentAttribute(
            id = UUID.randomUUID(),
            code = Some("YADA"),
            kind = PersistentAttributeDependency.Certified,
            description = "YADA",
            origin = Some("IPA"),
            name = "YADA",
            creationTime = OffsetDateTimeSupplier.get()
          ),
          PersistentAttributeDependency.PersistentAttribute(
            id = UUID.randomUUID(),
            code = Some(Digester.toSha256(admittedAttributeKind.getBytes())),
            kind = PersistentAttributeDependency.Certified,
            description = admittedAttributeKind,
            origin = Some("IPA"),
            name = admittedAttributeKind,
            creationTime = OffsetDateTimeSupplier.get()
          ),
          PersistentAttributeDependency.PersistentAttribute(
            id = UUID.randomUUID(),
            code = Some("104532"),
            kind = PersistentAttributeDependency.Certified,
            description = "104532",
            origin = Some("IPA"),
            name = "104532",
            creationTime = OffsetDateTimeSupplier.get()
          ),
          PersistentAttributeDependency.PersistentAttribute(
            id = UUID.randomUUID(),
            code = Some("OPA"),
            kind = PersistentAttributeDependency.Certified,
            description = "OPA",
            origin = Some("IPA"),
            name = "OPA",
            creationTime = OffsetDateTimeSupplier.get()
          ),
          PersistentAttributeDependency.PersistentAttribute(
            id = UUID.randomUUID(),
            code = Some("205942"),
            kind = PersistentAttributeDependency.Certified,
            description = "205942",
            origin = Some("IPA"),
            name = "205942",
            creationTime = OffsetDateTimeSupplier.get()
          )
        )

      mockFind[PersistentAttributeDependency.PersistentAttribute](
        "attributes",
        Filters.empty(),
        0,
        50,
        result = attributesfromRM
      )

      val expectedDelta: Set[AttributeSeed] = Set[AttributeSeed]()

      expectedDelta.foreach(expectedAttributeSeed =>
        mockCreateAttribute(
          expectedAttributeSeed,
          Attribute(
            id = UUID.randomUUID(),
            code = expectedAttributeSeed.code,
            kind = expectedAttributeSeed.kind,
            description = expectedAttributeSeed.description,
            origin = expectedAttributeSeed.origin,
            name = expectedAttributeSeed.name,
            creationTime = OffsetDateTimeSupplier.get()
          )
        )
      )

      jobs.loadCertifiedAttributes().futureValue(testTimeout) shouldEqual ()
    }
  }

}
