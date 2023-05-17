//package it.pagopa.interop.tenantsattributeschecker.service.impl
//
//import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
//import it.pagopa.interop.agreementprocess.client.api.AgreementApi
//import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
//import it.pagopa.interop.tenantsattributeschecker.service.{AgreementProcessInvoker, AgreementProcessService}
//
////import scala.concurrent.ExecutionContext
//
//final case class AgreementProcessServiceImpl(invoker: AgreementProcessInvoker, api: AgreementApi)(implicit
//  ec: ExecutionContext
//) extends AgreementProcessService {
//
//  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
//    Logger.takingImplicit[ContextFieldsToLog](this.getClass)
//
//}
