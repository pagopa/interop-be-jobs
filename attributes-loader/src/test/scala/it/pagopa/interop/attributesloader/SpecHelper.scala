package it.pagopa.interop.attributesloader

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.attributeregistryprocess.client.model.{Attribute, AttributeKind, AttributeSeed}
import it.pagopa.interop.attributesloader.service.{AttributeRegistryProcessService, PartyRegistryService}
import it.pagopa.interop.commons.cqrs.service.ReadModelService
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.{Digester, ORGANIZATION_ID_CLAIM, USER_ROLES}
import it.pagopa.interop.partyregistryproxy.client.model.{Categories, Category, Institution, Institutions}
import org.mongodb.scala.bson.conversions.Bson
import org.scalamock.scalatest.MockFactory
import spray.json._

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

trait SpecHelper extends SprayJsonSupport with DefaultJsonProtocol with MockFactory {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  final val bearerToken: String = "token"

  implicit val context: Seq[(String, String)] =
    Seq("bearer" -> bearerToken, USER_ROLES -> "internal", ORGANIZATION_ID_CLAIM -> UUID.randomUUID().toString)

  final val admittedAttributeKind: String = "Pubbliche Amministrazioni"

  val mockReadModel: ReadModelService                                      = mock[ReadModelService]
  val mockAttributeRegistryProcessService: AttributeRegistryProcessService =
    mock[AttributeRegistryProcessService]
  val mockPartyRegistryService: PartyRegistryService                       = mock[PartyRegistryService]

  val jobs: Jobs = new Jobs(mockAttributeRegistryProcessService, mockPartyRegistryService, mockReadModel)

  val categories: Categories =
    Categories(
      Seq(Category("YADA", "YADA", admittedAttributeKind, "IPA"), Category("OPA", "OPA", admittedAttributeKind, "IPA")),
      2
    )

  val institutions: Institutions = Institutions(
    Seq(
      Institution(
        id = "1111",
        originId = "104532",
        taxCode = "19530",
        category = "C7",
        description = "104532",
        digitalAddress = "test",
        address = "test",
        zipCode = "49300",
        origin = "IPA",
        kind = admittedAttributeKind
      ),
      Institution(
        id = "2222",
        originId = "205942",
        taxCode = "19530",
        category = "L8",
        description = "205942",
        digitalAddress = "test",
        address = "test",
        zipCode = "90142",
        origin = "IPA",
        kind = admittedAttributeKind
      )
    ),
    2
  )

  val attributeSeeds: Seq[AttributeSeed] = Seq(
    AttributeSeed(
      code = Some("YADA"),
      kind = AttributeKind.CERTIFIED,
      description = "YADA",
      origin = Some("IPA"),
      name = "YADA"
    ),
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
      code = Some("104532"),
      kind = AttributeKind.CERTIFIED,
      description = "104532",
      origin = Some("IPA"),
      name = "104532"
    ),
    AttributeSeed(
      code = Some("205942"),
      kind = AttributeKind.CERTIFIED,
      description = "205942",
      origin = Some("IPA"),
      name = "205942"
    )
  )

  def mockCreateAttribute(attributeSeed: AttributeSeed, result: Attribute)(implicit
    contexts: Seq[(String, String)]
  ): Unit = (mockAttributeRegistryProcessService
    .createAttribute(_: AttributeSeed)(_: String)(_: Seq[(String, String)]))
    .expects(attributeSeed, *, contexts)
    .once()
    .returns(Future.successful(result)): Unit

  def mockGetCategories(page: Option[Int] = None, limit: Option[Int] = None, result: Categories)(implicit
    contexts: Seq[(String, String)]
  ): Unit = (mockPartyRegistryService
    .getCategories(_: Option[Int], _: Option[Int])(_: Seq[(String, String)]))
    .expects(page, limit, contexts)
    .once()
    .returns(Future.successful(result)): Unit

  def mockGetInstitutions(page: Option[Int] = None, limit: Option[Int] = None, result: Institutions)(implicit
    contexts: Seq[(String, String)]
  ): Unit = (mockPartyRegistryService
    .getInstitutions(_: Option[Int], _: Option[Int])(_: Seq[(String, String)]))
    .expects(page, limit, contexts)
    .once()
    .returns(Future.successful(result)): Unit

  def mockFind[T](collectionName: String, filter: Bson, offset: Int, limit: Int, result: Seq[T]): Unit =
    (mockReadModel
      .find(_: String, _: Bson, _: Int, _: Int)(_: JsonReader[T], _: ExecutionContext))
      .expects(collectionName, filter, offset, limit, *, *)
      .once()
      .returns(Future.successful(result)): Unit
}
