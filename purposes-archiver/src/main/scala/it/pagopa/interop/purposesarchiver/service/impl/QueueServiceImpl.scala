package it.pagopa.interop.purposesarchiver.service.impl

import cats.syntax.all._
import it.pagopa.interop.purposesarchiver.service.QueueService
import it.pagopa.interop.commons.queue.message.Message

import scala.concurrent.{ExecutionContextExecutor, Future}
import it.pagopa.interop.commons.queue.impl.SQSReader
import it.pagopa.interop.agreementprocess.events.ArchiveEvent
import it.pagopa.interop.agreementprocess.events.Events._

final class QueueServiceImpl(queueName: String, visibilityTimeoutInSeconds: Int)(implicit
  blockingEc: ExecutionContextExecutor
) extends QueueService {

  val sQSReader: SQSReader = new SQSReader(queueName, visibilityTimeoutInSeconds)(f => _.convertTo[ArchiveEvent])

  override def processMessages(fn: Message => Future[Unit]): Future[Unit] =
    sQSReader.handleN(10)(fn).void
}
