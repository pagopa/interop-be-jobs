package it.pagopa.interop.tenantsattributeschecker.service.impl

import spray.json._

import scala.io.Source

final case class MailTemplate(subject: String, body: String)

object MailTemplate {

  private def getTemplate(resource: String): MailTemplate =
    Source.fromResource(resource).getLines().mkString.parseJson.convertTo[MailTemplate]

  val expiring: (MailTemplate, MailTemplate) =
    (getTemplate("mailTemplates/consumer-expiring-mail.json"), getTemplate("mailTemplates/producer-expiring-mail.json"))

  val expired: (MailTemplate, MailTemplate) =
    (getTemplate("mailTemplates/consumer-expired-mail.json"), getTemplate("mailTemplates/producer-expired-mail.json"))
}
