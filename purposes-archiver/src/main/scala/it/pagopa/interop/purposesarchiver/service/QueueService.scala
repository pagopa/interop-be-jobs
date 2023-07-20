package it.pagopa.interop.purposesarchiver.service

import scala.concurrent.Future

trait QueueService {
  def receive()(implicit contexts: Seq[(String, String)]): Future[Unit]
}
