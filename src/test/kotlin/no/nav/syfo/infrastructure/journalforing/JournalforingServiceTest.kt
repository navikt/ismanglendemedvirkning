package no.nav.syfo.infrastructure.journalforing

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT
import no.nav.syfo.UserConstants.PDF_FORHANDSVARSEL
import no.nav.syfo.UserConstants.PDF_STANS
import no.nav.syfo.UserConstants.PDF_VURDERING
import no.nav.syfo.domain.VurderingType
import no.nav.syfo.generator.generateJournalpostRequest
import no.nav.syfo.generator.generateVurdering
import no.nav.syfo.infrastructure.clients.dokarkiv.DokarkivClient
import no.nav.syfo.infrastructure.clients.dokarkiv.dto.BrevkodeType
import no.nav.syfo.infrastructure.clients.dokarkiv.dto.JournalpostType
import no.nav.syfo.infrastructure.mock.dokarkivResponse
import no.nav.syfo.infrastructure.mock.mockedJournalpostId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class JournalforingServiceTest {
    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val dokarkivMock = mockk<DokarkivClient>(relaxed = true)
    private val journalforingService = JournalforingService(
        dokarkivClient = dokarkivMock,
        pdlClient = externalMockEnvironment.pdlClient,
        isJournalforingRetryEnabled = externalMockEnvironment.environment.isJournalforingRetryEnabled,
    )

    @BeforeEach
    fun beforeEach() {
        clearAllMocks()
        coEvery { dokarkivMock.journalfor(any()) } returns dokarkivResponse
    }

    @Test
    fun `Journalforer OPPFYLT vurdering`() {
        val vurderingOppfylt = generateVurdering(type = VurderingType.OPPFYLT)
        val journalpostId = runBlocking {
            journalforingService.journalfor(
                personident = ARBEIDSTAKER_PERSONIDENT,
                pdf = PDF_VURDERING,
                vurdering = vurderingOppfylt,
            )
        }

        assertEquals(mockedJournalpostId, journalpostId)

        coVerify(exactly = 1) {
            dokarkivMock.journalfor(
                journalpostRequest = generateJournalpostRequest(
                    tittel = "Vurdering av § 8-8 manglende medvirkning",
                    brevkodeType = BrevkodeType.MANGLENDE_MEDVIRKNING_VURDERING,
                    pdf = PDF_VURDERING,
                    vurderingUuid = vurderingOppfylt.uuid,
                    journalpostType = JournalpostType.UTGAAENDE.name,
                )
            )
        }
    }

    @Test
    fun `Journalforer FORHANDSVARSEL vurdering`() {
        val vurderingForhandsvarsel = generateVurdering(type = VurderingType.FORHANDSVARSEL)
        val journalpostId = runBlocking {
            journalforingService.journalfor(
                personident = ARBEIDSTAKER_PERSONIDENT,
                pdf = PDF_FORHANDSVARSEL,
                vurdering = vurderingForhandsvarsel,
            )
        }

        assertEquals(mockedJournalpostId, journalpostId)

        coVerify(exactly = 1) {
            dokarkivMock.journalfor(
                journalpostRequest = generateJournalpostRequest(
                    tittel = "Forhåndsvarsel om stans av sykepenger",
                    brevkodeType = BrevkodeType.MANGLENDE_MEDVIRKNING_FORHANDSVARSEL,
                    pdf = PDF_FORHANDSVARSEL,
                    vurderingUuid = vurderingForhandsvarsel.uuid,
                    journalpostType = JournalpostType.UTGAAENDE.name,
                )
            )
        }
    }

    @Test
    fun `Journalforer STANS vurdering`() {
        val vurderingStans = generateVurdering(type = VurderingType.STANS)
        val journalpostId = runBlocking {
            journalforingService.journalfor(
                personident = ARBEIDSTAKER_PERSONIDENT,
                pdf = PDF_STANS,
                vurdering = vurderingStans,
            )
        }

        assertEquals(mockedJournalpostId, journalpostId)

        coVerify(exactly = 1) {
            dokarkivMock.journalfor(
                journalpostRequest = generateJournalpostRequest(
                    tittel = "Innstilling om stans",
                    brevkodeType = BrevkodeType.MANGLENDE_MEDVIRKNING_STANS,
                    pdf = PDF_STANS,
                    vurderingUuid = vurderingStans.uuid,
                    journalpostType = JournalpostType.NOTAT.name,
                )
            )
        }
    }

    @Test
    fun `Journalforer IKKE_AKTUELL vurdering`() {
        val vurderingIkkeAktuell = generateVurdering(type = VurderingType.IKKE_AKTUELL)
        val journalpostId = runBlocking {
            journalforingService.journalfor(
                personident = ARBEIDSTAKER_PERSONIDENT,
                pdf = PDF_VURDERING,
                vurdering = vurderingIkkeAktuell,
            )
        }

        assertEquals(mockedJournalpostId, journalpostId)

        coVerify(exactly = 1) {
            dokarkivMock.journalfor(
                journalpostRequest = generateJournalpostRequest(
                    tittel = "Vurdering av § 8-8 manglende medvirkning",
                    brevkodeType = BrevkodeType.MANGLENDE_MEDVIRKNING_VURDERING,
                    pdf = PDF_VURDERING,
                    vurderingUuid = vurderingIkkeAktuell.uuid,
                    journalpostType = JournalpostType.UTGAAENDE.name,
                )
            )
        }
    }

    @Test
    fun `Journalforer UNNTAK vurdering`() {
        val vurderingUnntak = generateVurdering(type = VurderingType.UNNTAK)
        val journalpostId = runBlocking {
            journalforingService.journalfor(
                personident = ARBEIDSTAKER_PERSONIDENT,
                pdf = PDF_VURDERING,
                vurdering = vurderingUnntak,
            )
        }

        assertEquals(mockedJournalpostId, journalpostId)

        coVerify(exactly = 1) {
            dokarkivMock.journalfor(
                journalpostRequest = generateJournalpostRequest(
                    tittel = "Vurdering av § 8-8 manglende medvirkning",
                    brevkodeType = BrevkodeType.MANGLENDE_MEDVIRKNING_VURDERING,
                    pdf = PDF_VURDERING,
                    vurderingUuid = vurderingUnntak.uuid,
                    journalpostType = JournalpostType.UTGAAENDE.name,
                )
            )
        }
    }
}
