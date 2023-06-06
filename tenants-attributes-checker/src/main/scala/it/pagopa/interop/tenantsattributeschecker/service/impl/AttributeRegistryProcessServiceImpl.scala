package it.pagopa.interop.tenantsattributeschecker.service.impl

import akka.actor.typed.ActorSystem
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.attributeregistryprocess.client.api.{AttributeApi, EnumsSerializers}
import it.pagopa.interop.attributeregistryprocess.client.invoker.{ApiInvoker, BearerToken}
import it.pagopa.interop.attributeregistryprocess.client.model.Attribute
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.withHeaders
import it.pagopa.interop.tenantsattributeschecker.ApplicationConfiguration
import it.pagopa.interop.tenantsattributeschecker.service.AttributeRegistryProcessService

import java.util.UUID
import scala.concurrent.{ExecutionContextExecutor, Future}

final case class AttributeRegistryProcessServiceImpl(blockingEc: ExecutionContextExecutor)(implicit
  system: ActorSystem[_]
) extends AttributeRegistryProcessService {

  private val invoker: ApiInvoker = ApiInvoker(EnumsSerializers.all, blockingEc)(system.classicSystem)
  private val api: AttributeApi   = AttributeApi(ApplicationConfiguration.attributeRegistryProcessURL)

  private implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  override def getAttributeById(attributeId: UUID)(implicit contexts: Seq[(String, String)]): Future[Attribute] =
    withHeaders[Attribute] { (bearerToken, correlationId, ip) =>
      val request = api.getAttributeById(xCorrelationId = correlationId, attributeId = attributeId, xForwardedFor = ip)(
        BearerToken(bearerToken)
      )
      invoker.invoke(request, s"Getting attribute by id $attributeId")
    }
}
