package it.pagopa.interop.purposesarchiver.service

import it.pagopa.interop.commons.queue.message.Message

import scala.concurrent.Future

trait QueueService {
  def processMessages(fn: Message => Future[Unit]): Future[Unit]
}
