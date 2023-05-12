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

import scala.concurrent.{Future, ExecutionContext}
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit

import java.util.UUID
import scala.util.Failure

object Main extends App {
  val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass.getCanonicalName())

  implicit val context: List[(String, String)] = (CORRELATION_ID_HEADER -> UUID.randomUUID().toString()) :: Nil

  implicit val scanamo: ScanamoAsync = ScanamoAsync(DynamoDbAsyncClient.create())(global)

  logger.info("Starting privacy notices updater job")

  def getOneTrustService(
    oneTrustConfiguration: OneTrustConfiguration
  )(implicit global: ExecutionContext): Future[(OneTrustService, WebSocketBackend[Future])] = {
    implicit val backend = Slf4jLoggingBackend(
      HttpClientFutureBackend(options =
        BackendOptions
          .connectionTimeout(FiniteDuration(oneTrustConfiguration.connectionTimeoutInSeconds, TimeUnit.SECONDS))
      )
    )
    Future((new OneTrustServiceImpl(oneTrustConfiguration)(global, backend, context), backend))
  }

  def getDynamoService(dynamoConfiguration: DynamoConfiguration)(implicit
    global: ExecutionContext
  ): Future[DynamoService] =
    Future(new DynamoServiceImpl(dynamoConfiguration)(global, scanamo))

  def resources()(implicit
    global: ExecutionContext
  ): Future[(Configuration, OneTrustService, DynamoService, WebSocketBackend[Future])] = {
    logger.info(s"Start resources")
    for {
      config <- Configuration.read()
      _ = logger.debug(s"Configuration is ${config}")
      (ots, bk) <- getOneTrustService(config.oneTrust)
      ds        <- getDynamoService(config.dynamo)
    } yield (config, ots, ds, bk)
  }

  def execution(config: Configuration, ots: OneTrustService, ds: DynamoService)(implicit
    global: ExecutionContext
  ): Future[Unit] = for {
    token  <- ots.getBearerToken()
    ppOts  <- ots.getById(config.oneTrust.ppUuid)(token.access_token)
    ppDs   <- ds.getById(config.oneTrust.ppUuid)
    _      <- ppOts.fold(ppDs.fold(Future.successful(()))(p => ds.delete(p.id)))(p => ds.put(p.toPersistent))
    tosOts <- ots.getById(config.oneTrust.tosUuid)(token.access_token)
    tosDs  <- ds.getById(config.oneTrust.tosUuid)
    _      <- tosOts.fold(tosDs.fold(Future.successful(()))(p => ds.delete(p.id)))(p => ds.put(p.toPersistent))
  } yield ()

  def app()(implicit global: ExecutionContext): Future[Unit] = resources()
    .andThen { case Failure(e) =>
      logger.error("privacy notices configuration got an error", e)
    }
    .flatMap { case (config, otm, ds, bk) =>
      execution(config, otm, ds)
        .andThen { case Failure(e) =>
          logger.error("privacy notices updater got an error", e)
        }
        .andThen { _ =>
          bk.close()
        }
    }

  Await.ready(app()(global), Duration.Inf): Unit
  logger.info("Completed privacy notices updater job")
}
