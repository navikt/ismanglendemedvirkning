package no.nav.syfo.infrastructure.dokarkiv

import kotlinx.coroutines.runBlocking
import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.UserConstants.EXISTING_EKSTERN_REFERANSE_UUID
import no.nav.syfo.UserConstants.PDF_FORHANDSVARSEL
import no.nav.syfo.generator.generateJournalpostRequest
import no.nav.syfo.infrastructure.clients.dokarkiv.dto.BrevkodeType
import no.nav.syfo.infrastructure.mock.dokarkivConflictResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

class DokarkivClientTest {
    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val dokarkivClient = externalMockEnvironment.dokarkivClient

    @Test
    fun `journalforer forhandsvarsel`() {
        val journalpostRequestForhandsvarsel = generateJournalpostRequest(
            tittel = "Forhåndsvarsel om stans av sykepenger",
            brevkodeType = BrevkodeType.MANGLENDE_MEDVIRKNING_FORHANDSVARSEL,
            pdf = PDF_FORHANDSVARSEL,
            vurderingUuid = UUID.randomUUID(),
        )

        runBlocking {
            val response = dokarkivClient.journalfor(
                journalpostRequest = journalpostRequestForhandsvarsel,
            )

            assertEquals(1, response.journalpostId)
        }
    }

    @Test
    fun `handles conflict from api when eksternRefeanseId exists, and uses the existing journalpostId`() {
        val journalpostRequestForhandsvarsel = generateJournalpostRequest(
            tittel = "Forhåndsvarsel om stans av sykepenger",
            brevkodeType = BrevkodeType.MANGLENDE_MEDVIRKNING_FORHANDSVARSEL,
            pdf = PDF_FORHANDSVARSEL,
            vurderingUuid = EXISTING_EKSTERN_REFERANSE_UUID,
        )

        runBlocking {
            val journalpostResponse =
                dokarkivClient.journalfor(journalpostRequest = journalpostRequestForhandsvarsel)

            assertEquals(dokarkivConflictResponse.journalpostId, journalpostResponse.journalpostId)
            assertEquals(dokarkivConflictResponse.journalstatus, journalpostResponse.journalstatus)
        }
    }
}
