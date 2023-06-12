package it.pagopa.interop.tenantsattributeschecker.service.impl

import spray.json._

import scala.io.{BufferedSource, Source}

final case class MailTemplate(subject: String, body: String)

object MailTemplate {

  val expiring: (MailTemplate, MailTemplate) = {
    val consumer: BufferedSource = Source.fromResource("mailTemplates/activation/consumer-expiring-mail.json")
    val producer: BufferedSource = Source.fromResource("mailTemplates/activation/producer-expiring-mail.json")
    (
      consumer.getLines().mkString.parseJson.convertTo[MailTemplate],
      producer.getLines().mkString.parseJson.convertTo[MailTemplate]
    )
  }

  val expired: (MailTemplate, MailTemplate) = {
    val consumer: BufferedSource = Source.fromResource("mailTemplates/activation/consumer-expired-mail.json")
    val producer: BufferedSource = Source.fromResource("mailTemplates/activation/producer-expired-mail.json")
    (
      consumer.getLines().mkString.parseJson.convertTo[MailTemplate],
      producer.getLines().mkString.parseJson.convertTo[MailTemplate]
    )
  }

}
