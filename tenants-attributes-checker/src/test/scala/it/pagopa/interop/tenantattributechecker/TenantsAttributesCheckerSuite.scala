package it.pagopa.interop.tenantattributechecker

import it.pagopa.interop.attributeregistryprocess.client.model.Attribute
import it.pagopa.interop.certifiedMailSender.InteropEnvelope
import it.pagopa.interop.tenantattributechecker.utils.SpecHelper
import it.pagopa.interop.tenantmanagement.model.tenant.PersistentTenant
import it.pagopa.interop.tenantprocess.client.model._
import it.pagopa.interop.tenantsattributeschecker.util.Jobs
import org.scalatest.funsuite._

import java.util.UUID
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class TenantsAttributesCheckerSuite extends AnyFunSuite with SpecHelper {

  test("sendEnvelope for expiring attributes") {
    val attribute: Attribute       = mockAttribute
    val producer: Tenant           = mockTenant
    val consumer: PersistentTenant = createPersistentTenant(UUID.randomUUID(), attribute.id, producer.id)
    val producerSelfcare           = institution.copy(externalId = producer.id.toString)
    val consumerSelfcare           = institution.copy(externalId = consumer.id.toString)
    val envelopeUUID               = UUID.randomUUID()

    val consumerEnvelope: InteropEnvelope = InteropEnvelope(
      id = envelopeUUID,
      recipients = List(consumerSelfcare.digitalAddress),
      subject = "Scadenza attributo in 30 giorni",
      body =
        s"""|<!DOCTYPE html>
            |<html lang="it">
            |
            |<head>
            |    <meta charset="UTF-8">
            |    <meta http-equiv="X-UA-Compatible" content="IE=edge">
            |    <meta name="viewport" content="width=device-width, initial-scale=1.0">
            |</head>
            |
            |<body>
            |    <div style="white-space: pre-line;">
            |        Ciao,
            |
            |        l'attributo <strong>${attribute.name}</strong> che ti è stato riconosciuto dall'ente <strong>${producer.name}</strong> scadrà tra 30 giorni.
            |
            |L'attributo ti verrà revocato e questo potrebbe avere impatti sullo stato di alcune tue richieste di fruizione.
            |
            |Per monitorare lo stato dei tuoi attributi, <a href="https://selfcare.pagopa.it/" title="Accedi a PDND Interoperabilità">accedi a PDND Interoperabilità</a>.
            |
            |A presto,
            |Il team di PDND Interoperabilità
            |    </div>
            |
            |</body>
            |
            |</html>""".stripMargin,
      attachments = List.empty
    )
    val producerEnvelope: InteropEnvelope = InteropEnvelope(
      id = envelopeUUID,
      recipients = List(producerSelfcare.digitalAddress),
      subject = "Scadenza attributo in 30 giorni",
      body =
        s"""<!DOCTYPE html>\n<html lang=\"it\">\n\n<head>\n    <meta charset=\"UTF-8\">\n    <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\n    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n</head>\n\n<body>\n    <div style=\"white-space: pre-line;\">\n        Ciao,\n\n        l'attributo <strong>${attribute.name}</strong> che hai riconosciuto all'ente <strong>${consumer.name}</strong> scadrà tra 30 giorni.\n\nL'attributo sarà revocato e questo potrebbe avere impatti sullo stato di alcune sue richieste di fruizione.\n\nPer monitorare lo stato degli attributi, <a href=\"https://selfcare.pagopa.it/\" title=\"Accedi a PDND Interoperabilità\">accedi a PDND Interoperabilità</a>.\n\nA presto,\nIl team di PDND Interoperabilità\n    </div>\n\n</body>\n\n</html>""",
      attachments = List.empty
    )

    val jobs = new Jobs(
      agreementProcess = mockAgreementProcess,
      tenantProcess = mockTenantProcess,
      attributeRegistryProcess = mockAttributeRegistryProcess,
      queueService = mockQueueService,
      selfcareClientService = mockSelfcareClient,
      () => envelopeUUID
    )

    setupMocks(attribute, producer, consumer, producerSelfcare, consumerSelfcare, producerEnvelope, consumerEnvelope)

    // Run the method under test
    val result: Future[Unit] =
      jobs.sendEnvelope(
        attribute.id,
        consumer,
        persistentTenantVerifier(producer.id),
        consumerExpiringTemplate,
        producerExpiringTemplate
      )

    Await.ready(result, Duration.Inf)
  }
}
