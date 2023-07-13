package it.pagopa.interop.tenantsattributeschecker.service.impl

import it.pagopa.interop.commons.queue.impl.SQSHandler
import it.pagopa.interop.tenantsattributeschecker.service.QueueService
import spray.json.JsonWriter

import scala.concurrent.Future

final class QueueServiceImpl(sqsHandler: SQSHandler) extends QueueService {

  override def send[T: JsonWriter](message: T): Future[String] = sqsHandler.send(message)
}
