package it.pagopa.interop.tenantsattributeschecker.service.impl

import it.pagopa.interop.commons.queue.impl.SQSHandler
import it.pagopa.interop.tenantsattributeschecker.service.QueueService
import spray.json.JsonWriter

import scala.concurrent.{ExecutionContextExecutor, Future}

final class QueueServiceImpl(queueUrl: String)(implicit blockingEc: ExecutionContextExecutor) extends QueueService {

  val sqsHandler: SQSHandler = SQSHandler(queueUrl)(blockingEc)

  override def send[T: JsonWriter](message: T): Future[String] = sqsHandler.send(message)
}
