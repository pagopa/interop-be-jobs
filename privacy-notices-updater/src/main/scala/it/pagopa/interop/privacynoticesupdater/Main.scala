package it.pagopa.interop.privacynoticesupdater

import com.typesafe.scalalogging.Logger
import it.pagopa.interop.commons.logging._
import com.typesafe.scalalogging.LoggerTakingImplicit
import it.pagopa.interop.commons.utils.CORRELATION_ID_HEADER
import org.scanamo.ScanamoAsync
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import sttp.client4.logging.slf4j.Slf4jLoggingBackend
import sttp.client4.httpclient.HttpClientFutureBackend
import sttp.client4.{BackendOptions, WebSocketBackend}

import it.pagopa.interop.privacynoticesupdater.util._
import it.pagopa.interop.privacynoticesupdater.service._
import it.pagopa.interop.privacynoticesupdater.converters.PrivacyNoticeConverter._

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future, Await}
import java.util.concurrent.{ExecutorService, Executors, TimeUnit}
import scala.concurrent.duration._

import java.util.UUID
import scala.util.Failure
import scala.concurrent.ExecutionContext.global

object Main extends App {
  val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass.getCanonicalName())

  implicit val context: List[(String, String)] = (CORRELATION_ID_HEADER -> UUID.randomUUID().toString()) :: Nil

  val blockingThreadPool: ExecutorService           =
    Executors.newFixedThreadPool(1.max(Runtime.getRuntime.availableProcessors() - 1))
  implicit val blockingEC: ExecutionContextExecutor = ExecutionContext.fromExecutor(blockingThreadPool)

  implicit val scanamo: ScanamoAsync = ScanamoAsync(DynamoDbAsyncClient.create())(blockingEC)

  logger.info("Starting privacy notices updater job")

  def getOneTrustService(
    oneTrustConfiguration: OneTrustConfiguration
  ): Future[(OneTrustService, WebSocketBackend[Future])] = Future {
    val backend: WebSocketBackend[Future] = Slf4jLoggingBackend(
      HttpClientFutureBackend(options =
        BackendOptions
          .connectionTimeout(FiniteDuration(oneTrustConfiguration.connectionTimeoutInSeconds, TimeUnit.SECONDS))
      )(blockingEC)
    )
    (new OneTrustServiceImpl(oneTrustConfiguration)(blockingEC, backend, context), backend)
  }

  def getDynamoService(dynamoConfiguration: DynamoConfiguration): Future[DynamoService] =
    Future(new DynamoServiceImpl(dynamoConfiguration)(blockingEC, scanamo))

  def resources(): Future[(Configuration, OneTrustService, DynamoService, WebSocketBackend[Future])] = {
    logger.info(s"Start resources")
    for {
      config <- Configuration.read()
      _ = logger.debug(s"Configuration is ${config}")
      (ots, bk) <- getOneTrustService(config.oneTrust)
      ds        <- getDynamoService(config.dynamo)
    } yield (config, ots, ds, bk)
  }

  def execution(config: Configuration, ots: OneTrustService, ds: DynamoService): Future[Unit] = for {
    token  <- ots.getBearerToken()
    ppOts  <- ots.getById(config.oneTrust.ppUuid)(token.access_token)
    ppDs   <- ds.getById(config.oneTrust.ppUuid)
    _      <- ppOts
      .map(p => ds.put(p.toPersistent))
      .orElse(ppDs.map(p => ds.delete(p.pnId)))
      .getOrElse(Future.unit)
    tosOts <- ots.getById(config.oneTrust.tosUuid)(token.access_token)
    tosDs  <- ds.getById(config.oneTrust.tosUuid)
    _      <- tosOts
      .map(p => ds.put(p.toPersistent))
      .orElse(tosDs.map(p => ds.delete(p.pnId)))
      .getOrElse(Future.unit)
  } yield ()

  def app(): Future[Unit] = resources()
    .andThen { case Failure(e) =>
      logger.error("Privacy notices configuration got an error", e)
    }
    .flatMap { case (config, otm, ds, bk) =>
      execution(config, otm, ds)
        .andThen { case Failure(e) =>
          logger.error("privacy notices updater got an error", e)
        }
        .andThen { _ =>
          bk.close().map(_ => blockingThreadPool.shutdown())(global)
        }(global)
    }(global)

  Await.ready(app(), Duration.Inf): Unit
  logger.info("Completed privacy notices updater job")
}
