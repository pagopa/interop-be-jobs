package it.pagopa.interop.purposesarchiver.service.impl

import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.agreementprocess.client.model.Agreement
import it.pagopa.interop.agreementprocess.client.api.AgreementApi
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.agreementprocess.client.invoker.{
  ApiRequest,
  BearerToken,
  ApiInvoker => AgreementProcessInvoker
}
import it.pagopa.interop.purposesarchiver.service.AgreementProcessService
import it.pagopa.interop.commons.utils.withHeaders

import java.util.UUID
import scala.concurrent.Future

final case class AgreementProcessServiceImpl(invoker: AgreementProcessInvoker, api: AgreementApi)
    extends AgreementProcessService {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass)

  override def getAgreementById(agreementId: UUID)(implicit contexts: Seq[(String, String)]): Future[Agreement] =
    withHeaders { (bearerToken, correlationId, ip) =>
      val request: ApiRequest[Agreement] =
        api.getAgreementById(xCorrelationId = correlationId, agreementId = agreementId.toString, xForwardedFor = ip)(
          BearerToken(bearerToken)
        )
      invoker.invoke(request, s"Retrieving agreement $agreementId")
    }
}
