package it.pagopa.interop.eservicedescriptorsarchiver

import cats.syntax.all._
import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.cqrs.service.MongoDbReadModelService
import it.pagopa.interop.commons.jwt.service.InteropTokenGenerator
import it.pagopa.interop.commons.jwt.service.impl.DefaultInteropTokenGenerator
import it.pagopa.interop.commons.jwt.{JWTConfiguration, JWTInternalTokenConfig, KID, PrivateKeysKidHolder}
import it.pagopa.interop.commons.logging.{CanLogContextFields, ContextFieldsToLog}
import it.pagopa.interop.commons.queue.config.SQSHandlerConfig
import it.pagopa.interop.commons.queue.impl.SQSHandler
import it.pagopa.interop.commons.signer.service.SignerService
import it.pagopa.interop.commons.signer.service.impl.KMSSignerService
import it.pagopa.interop.commons.utils.{BEARER, CORRELATION_ID_HEADER}
import it.pagopa.interop.eservicedescriptorsarchiver.service.impl.CatalogProcessServiceImpl
import it.pagopa.interop.eservicedescriptorsarchiver.util.JobExecution

import java.util.UUID
import java.util.concurrent.{ExecutorService, Executors}
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

trait Dependencies {

  val blockingThreadPool: ExecutorService  = Executors.newFixedThreadPool(Runtime.getRuntime.availableProcessors())
  val blockingEC: ExecutionContextExecutor = ExecutionContext.fromExecutor(blockingThreadPool)

  implicit val ec: ExecutionContext            = blockingEC
  implicit val context: List[(String, String)] = createContext()

  def createContext(bearer: Option[String] = None): List[(String, String)] = {
    val cid = CORRELATION_ID_HEADER -> UUID.randomUUID().toString
    bearer.fold(cid :: Nil)(b => cid :: (BEARER -> b) :: Nil)
  }

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass.getCanonicalName)

  def processMessages(messages: List[String], receipts: List[String]): Future[Unit] = {
    logger.info(s"Processing ${messages.size} messages")
    for {
      bearer <- generateBearer(jwtConfig, signerService(blockingEC))
      job    <- jobExecution(createContext(bearer.some))
      _      <- Future.traverse(messages)(job.archiveEservice)
      _      <- Future.traverse(receipts)(sqsHandler.deleteMessage)
      _ = logger.info(s"${messages.size} messages processed")
    } yield ()
  }

  private val jwtConfig: JWTInternalTokenConfig = JWTConfiguration.jwtInternalTokenConfig

  private def signerService(blockingEc: ExecutionContextExecutor): SignerService = new KMSSignerService(blockingEc)

  private def generateBearer(jwtConfig: JWTInternalTokenConfig, signerService: SignerService)(implicit
    ec: ExecutionContext
  ): Future[String] = for {
    tokenGenerator <- interopTokenGenerator(signerService)
    m2mToken       <- tokenGenerator
      .generateInternalToken(
        subject = jwtConfig.subject,
        audience = jwtConfig.audience.toList,
        tokenIssuer = jwtConfig.issuer,
        secondsDuration = jwtConfig.durationInSeconds
      )
  } yield m2mToken.serialized

  private def interopTokenGenerator(
    signerService: SignerService
  )(implicit ec: ExecutionContext): Future[InteropTokenGenerator] = Future(
    new DefaultInteropTokenGenerator(
      signerService,
      new PrivateKeysKidHolder {
        override val RSAPrivateKeyset: Set[KID] = ApplicationConfiguration.rsaKeysIdentifiers
        override val ECPrivateKeyset: Set[KID]  = ApplicationConfiguration.ecKeysIdentifiers
      }
    )
  )

  private val sqsHandlerConfig: SQSHandlerConfig =
    SQSHandlerConfig(
      queueUrl = ApplicationConfiguration.archivingEservicesQueueUrl,
      visibilityTimeout = ApplicationConfiguration.visibilityTimeout
    )

  val sqsHandler: SQSHandler = SQSHandler(sqsHandlerConfig)(blockingEC)

  final val readModelService: MongoDbReadModelService = new MongoDbReadModelService(
    ApplicationConfiguration.readModelConfig
  )

  final val catalogProcessService: CatalogProcessServiceImpl = CatalogProcessServiceImpl(blockingEC)

  def jobExecution(context: List[(String, String)]): Future[JobExecution] =
    Future.successful(JobExecution(readModelService, catalogProcessService)(ec, context))

}
