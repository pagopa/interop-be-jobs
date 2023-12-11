package it.pagopa.interop.metricsreportgenerator

import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.cqrs.service.{MongoDbReadModelService, ReadModelService}
import it.pagopa.interop.commons.files.service.FileManager
import it.pagopa.interop.commons.logging._
import it.pagopa.interop.commons.mail.{InteropMailer, MailAttachment, TextMail}
import it.pagopa.interop.commons.utils.CORRELATION_ID_HEADER
import it.pagopa.interop.metricsreportgenerator.util._
import spoiwo.model.Workbook
import spoiwo.natures.xlsx.Model2XlsxConversions._

import java.util.UUID
import java.util.concurrent.{ExecutorService, Executors}
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.Failure
import java.io.ByteArrayOutputStream

object Main extends App {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass.getCanonicalName())

  implicit val context: List[(String, String)] = (CORRELATION_ID_HEADER -> UUID.randomUUID().toString()) :: Nil

  logger.info("Starting metrics report generator job")

  def resources(implicit ec: ExecutionContext): Future[
    (ExecutorService, ExecutionContextExecutor, FileManager, S3, ReadModelService, Jobs, TokensJobs, Configuration)
  ] = for {
    config <- Configuration.read()
    es     <- Future(Executors.newFixedThreadPool(1.max(Runtime.getRuntime.availableProcessors() - 1)))
    blockingEC = ExecutionContext.fromExecutor(es)
    fm        <- Future(FileManager.get(FileManager.S3)(blockingEC))
    s3        <- Future(new S3(fm, config))
    rm        <- Future(new MongoDbReadModelService(config.readModel))
    jobs      <- Future(new Jobs(config, rm))
    tokensJob <- Future(new TokensJobs(s3))
  } yield (es, blockingEC, fm, s3, rm, jobs, tokensJob, config)

  def sendMail(config: Configuration, attachments: Seq[MailAttachment]): Future[Unit] = {
    val mail: TextMail =
      TextMail(
        UUID.randomUUID(),
        config.recipients,
        s"Report ${config.environment}",
        s"Data report of ${config.environment}",
        attachments
      )
    InteropMailer.from(config.mailer).send(mail)
  }

  def asAttachment(fileName: String, content: Array[Byte]): MailAttachment =
    MailAttachment(fileName, content, "text/csv")

  def execution(jobs: Jobs, tokensJob: TokensJobs, s3: S3, config: Configuration)(implicit
    blockingEC: ExecutionContextExecutor
  ): Future[Unit] = {
    val env: String = config.environment
    for {

      _ <- jobs.getAgreementRecord
        .map(_.getBytes)
        .flatMap(bs => s3.saveAgreementsReport(bs))

      _ <- jobs.getDescriptorsRecord
        .map(_.getBytes)
        .flatMap(bs => s3.saveActiveDescriptorsReport(bs))

      _ <- tokensJob.getTokensDataCsv
        .map(_.getBytes)
        .flatMap(bs => s3.saveTokensReport(bs))

      agreementsSheet  <- jobs.getAgreementSheet
      descriptorsSheet <- jobs.getDescriptorsSheet
      tokenSheet       <- tokensJob.getTokensDataSheet
      _                <- sendMail(
        config,
        Seq(
          asAttachment(
            s"report-${env}.xlsx",
            Workbook(agreementsSheet, descriptorsSheet, tokenSheet)
              .writeToOutputStream(new ByteArrayOutputStream())
              .toByteArray()
          )
        )
      )
    } yield ()
  }

  def job(implicit ec: ExecutionContext): Future[Unit] = resources.flatMap {
    case (es, blockingEC, fm, s3, rm, jobs, tokensJob, config) =>
      execution(jobs, tokensJob, s3, config)(blockingEC)
        .andThen { case Failure(ex) => logger.error("Metrics job got an error", ex) }
        .andThen { _ =>
          es.shutdown()
          fm.close()
          rm.close()
        }
  }

  Await.result(job(global), Duration.Inf): Unit
  logger.info("Completed metrics report generator job")
}
