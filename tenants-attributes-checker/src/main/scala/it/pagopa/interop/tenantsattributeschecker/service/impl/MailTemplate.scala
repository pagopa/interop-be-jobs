package it.pagopa.interop.tenantsattributeschecker.service.impl

import spray.json._

import scala.io.{BufferedSource, Source}

final case class MailTemplate(subject: String, body: String)

object MailTemplate {

  def expiration(): (MailTemplate, MailTemplate) = {
    val consumer: BufferedSource = Source.fromResource("mailTemplates/activation/consumer-expiration-mail.json")
    val provider: BufferedSource = Source.fromResource("mailTemplates/activation/provider-expiration-mail.json")
    (
      consumer.getLines().mkString.parseJson.convertTo[MailTemplate],
      provider.getLines().mkString.parseJson.convertTo[MailTemplate]
    )
  }

  def expired(): (MailTemplate, MailTemplate) = {
    val consumer: BufferedSource = Source.fromResource("mailTemplates/activation/consumer-expired-mail.json")
    val provider: BufferedSource = Source.fromResource("mailTemplates/activation/provider-expired-mail.json")
    (
      consumer.getLines().mkString.parseJson.convertTo[MailTemplate],
      provider.getLines().mkString.parseJson.convertTo[MailTemplate]
    )
  }

}
