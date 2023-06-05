package it.pagopa.interop.tenantsattributeschecker.service.impl

import spray.json._

import scala.io.{BufferedSource, Source}

final case class MailTemplate(subject: String, body: String)

object MailTemplate {

  def expiration(): MailTemplate = {
    val source: BufferedSource = Source.fromResource("mailTemplates/activation/expiration-mail.json")
    source.getLines().mkString.parseJson.convertTo[MailTemplate]
  }

  def expired(): MailTemplate = {
    val source: BufferedSource = Source.fromResource("mailTemplates/activation/expired-mail.json")
    source.getLines().mkString.parseJson.convertTo[MailTemplate]
  }

}
