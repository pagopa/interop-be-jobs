package it.pagopa.interop.tenantsattributeschecker.service.impl

import spray.json._

import scala.io.{BufferedSource, Source}

final case class MailTemplate(subject: String, body: String)

object MailTemplate {

  val expiration: (MailTemplate, MailTemplate) = {
    val consumer: BufferedSource = Source.fromResource("mailTemplates/activation/consumer-expiration-mail.json")
    val producer: BufferedSource = Source.fromResource("mailTemplates/activation/producer-expiration-mail.json")
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
