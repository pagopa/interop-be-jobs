package it.pagopa.interop.metricsreportgenerator

import com.typesafe.scalalogging.{Logger, LoggerTakingImplicit}
import it.pagopa.interop.commons.files.service.FileManager
import it.pagopa.interop.metricsreportgenerator.util._
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.Duration
import java.util.concurrent.{ExecutorService, Executors}
import java.util.UUID
import it.pagopa.interop.commons.logging._
import it.pagopa.interop.commons.utils.CORRELATION_ID_HEADER
import it.pagopa.interop.commons.cqrs.service.{ReadModelService, MongoDbReadModelService}
import scala.util.Failure
import it.pagopa.interop.commons.mail.{InteropMailer, TextMail}
import it.pagopa.interop.commons.mail.MailAttachment

object Main extends App {

  implicit val logger: LoggerTakingImplicit[ContextFieldsToLog] =
    Logger.takingImplicit[ContextFieldsToLog](this.getClass.getCanonicalName())

  implicit val context: List[(String, String)] = (CORRELATION_ID_HEADER -> UUID.randomUUID().toString()) :: Nil

  logger.info("Starting metrics report generator job")

  def resources(implicit
    ec: ExecutionContext
  ): Future[(ExecutorService, ExecutionContextExecutor, FileManager, ReadModelService, Jobs, Configuration)] = for {
    config <- Configuration.read()
    es     <- Future(Executors.newFixedThreadPool(1.max(Runtime.getRuntime.availableProcessors() - 1)))
    blockingEC = ExecutionContext.fromExecutor(es)
    fm   <- Future(FileManager.get(FileManager.S3)(blockingEC))
    rm   <- Future(new MongoDbReadModelService(config.readModel))
    jobs <- Future(new Jobs(config, fm, rm))
  } yield (es, blockingEC, fm, rm, jobs, config)

  def sendMail(config: Configuration): List[MailAttachment] => Future[Unit] = ats => {
    val mail: TextMail =
      TextMail(config.recipients, s"Report ${config.environment}", s"Data report of ${config.environment}", ats)
    InteropMailer.from(config.mailer).send(mail)
  }

  def asAttachment(fileName: String, lines: List[String]): MailAttachment =
    MailAttachment(fileName, lines.mkString("\n").getBytes(), "text/csv")

  def execution(jobs: Jobs, config: Configuration)(implicit blockingEC: ExecutionContextExecutor): Future[Unit] = {
    val env: String = config.environment

    val job1: Future[MailAttachment] = jobs.getAgreementRecord
      .flatMap(jobs.store(s"agreements-${env}.csv", _))
      .map(asAttachment(s"agreements-${env}.csv", _))

    val job2: Future[MailAttachment] = jobs.getTokensData
      .flatMap(jobs.store(s"tokens-${env}.csv", _))
      .map(asAttachment(s"tokens-${env}.csv", _))

    val jobD: Future[(List[String], List[String])] = jobs.getDescriptorsRecord

    val job3: Future[MailAttachment] = jobD.flatMap { case (ds, _) =>
      jobs.store(s"descriptors-${env}.csv", ds).map(_ => asAttachment(s"descriptors-${env}.csv", ds))
    }

    val job4: Future[MailAttachment] = jobD.flatMap { case (_, ads) =>
      jobs.store(s"active-descriptors-${env}.csv", ads).map(_ => asAttachment(s"active-descriptors-${env}.csv", ads))
    }

    val attachments: Future[List[MailAttachment]] =
      Future.foldLeft(List(job1, job2, job3, job4))(List.empty[MailAttachment])(_.prepended(_))

    attachments.flatMap(sendMail(config))
  }

  def job(implicit ec: ExecutionContext): Future[Unit] = resources.flatMap {
    case (es, blockingEC, fm, rm, jobs, config) =>
      execution(jobs, config)(blockingEC)
        .andThen { case Failure(ex) => logger.error("Metrics job got an error", ex) }
        .andThen { _ =>
          es.shutdown()
          fm.close()
          rm.close()
        }
  }

  Await.result(job(global), Duration.Inf)
  logger.info("Completed metrics report generator job")
}
