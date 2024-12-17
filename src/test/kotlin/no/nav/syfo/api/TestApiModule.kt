package no.nav.syfo.api

import io.ktor.server.application.*
import io.mockk.mockk
import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.application.VurderingService
import no.nav.syfo.infrastructure.clients.pdfgen.VurderingPdfService
import no.nav.syfo.infrastructure.clients.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.infrastructure.journalforing.JournalforingService
import no.nav.syfo.infrastructure.kafka.VurderingProducer
import no.nav.syfo.infrastructure.kafka.VurderingRecord
import org.apache.kafka.clients.producer.KafkaProducer

fun Application.testApiModule(
    externalMockEnvironment: ExternalMockEnvironment,
) {
    val database = externalMockEnvironment.database
    val veilederTilgangskontrollClient = VeilederTilgangskontrollClient(
        azureAdClient = externalMockEnvironment.azureAdClient,
        clientEnvironment = externalMockEnvironment.environment.clients.istilgangskontroll,
        httpClient = externalMockEnvironment.mockHttpClient,
    )
    val mockVurderingProducer = mockk<KafkaProducer<String, VurderingRecord>>(relaxed = true)
    val vurderingProducer = VurderingProducer(producer = mockVurderingProducer)
    val vurderingPdfService = VurderingPdfService(
        pdfGenClient = externalMockEnvironment.pdfgenClient,
        pdlClient = externalMockEnvironment.pdlClient,
    )
    val journalforingService = JournalforingService(
        dokarkivClient = externalMockEnvironment.dokarkivClient,
        pdlClient = externalMockEnvironment.pdlClient,
        journalforingRetryEnabeled = externalMockEnvironment.environment.journalforingRetryEnabled,
    )
    val vurderingService =
        VurderingService(
            journalforingService = journalforingService,
            vurderingRepository = externalMockEnvironment.vurderingRepository,
            vurderingProducer = vurderingProducer,
            vurderingPdfService = vurderingPdfService,
        )

    this.apiModule(
        applicationState = externalMockEnvironment.applicationState,
        environment = externalMockEnvironment.environment,
        wellKnownInternalAzureAD = externalMockEnvironment.wellKnownInternalAzureAD,
        database = database,
        veilederTilgangskontrollClient = veilederTilgangskontrollClient,
        vurderingService = vurderingService,
    )
}
