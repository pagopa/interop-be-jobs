package it.pagopa.interop.tenantsattributeschecker.service.impl

import akka.actor.typed.ActorSystem
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.agreementprocess.client.api.{AgreementApi, EnumsSerializers}
import it.pagopa.interop.agreementprocess.client.invoker.{ApiInvoker, BearerToken}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.utils.TypeConversions.EitherOps
import it.pagopa.interop.commons.utils.extractHeaders
import it.pagopa.interop.tenantsattributeschecker.ApplicationConfiguration
import it.pagopa.interop.tenantsattributeschecker.service.AgreementProcessService

import java.util.UUID
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

// Note: The service takes a blocking execution context in order to implement a fire and forget call of computeAgreementsByAttribute
final case class AgreementProcessServiceImpl(blockingEc: ExecutionContextExecutor)(implicit system: ActorSystem[_])
    extends AgreementProcessService {

  private val invoker: ApiInvoker = ApiInvoker(EnumsSerializers.all, blockingEc)(system.classicSystem)
  private val api: AgreementApi   = AgreementApi(ApplicationConfiguration.agreementProcessURL)

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  override def computeAgreementsByAttribute(consumerId: UUID, attributeId: UUID)(implicit
    contexts: Seq[(String, String)]
  ): Future[Unit] = {
    implicit val ec: ExecutionContext = blockingEc

    for {
      (bearerToken, correlationId, ip) <- extractHeaders(contexts).toFuture
      request = api.computeAgreementsByAttribute(
        xCorrelationId = correlationId,
        consumerId = consumerId,
        attributeId = attributeId,
        xForwardedFor = ip
      )(BearerToken(bearerToken))
      result <- invoker
        .invoke(request, s"Agreements state compute triggered for Tenant $consumerId and Attribute $attributeId")
    } yield result
  }
}
