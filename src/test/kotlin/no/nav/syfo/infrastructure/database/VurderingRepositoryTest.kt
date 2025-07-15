package no.nav.syfo.infrastructure.database

import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT
import no.nav.syfo.UserConstants.VEILEDER_IDENT
import no.nav.syfo.domain.Varsel
import no.nav.syfo.domain.Vurdering
import no.nav.syfo.infrastructure.database.repository.VurderingRepository
import no.nav.syfo.util.nowUTC
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import java.util.UUID

class VurderingRepositoryTest {
    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val vurderingRepository = VurderingRepository(database = database)
    private val pdf = byteArrayOf(0x2E, 100)

    @AfterEach
    fun afterEach() {
        database.resetDatabase()
    }

    @Test
    fun `saves a vurdering`() {
        val vurdering = Vurdering.Oppfylt(
            uuid = UUID.randomUUID(),
            personident = ARBEIDSTAKER_PERSONIDENT,
            veilederident = VEILEDER_IDENT,
            createdAt = nowUTC(),
            begrunnelse = "Begrunnelse",
            document = emptyList(),
            journalpostId = null,
        )
        val savedVurdering = vurderingRepository.saveManglendeMedvirkningVurdering(vurdering, pdf)
        assertEquals(vurdering.personident, savedVurdering.personident)
        assertEquals(vurdering.veilederident, savedVurdering.veilederident)
        assertEquals(vurdering.begrunnelse, savedVurdering.begrunnelse)
        assertEquals(vurdering.document, savedVurdering.document)
        assertEquals(vurdering.journalpostId, savedVurdering.journalpostId)
    }

    @Test
    fun `saves a vurdering of type STANS`() {
        val vurdering = Vurdering.Stans(
            uuid = UUID.randomUUID(),
            personident = ARBEIDSTAKER_PERSONIDENT,
            veilederident = VEILEDER_IDENT,
            createdAt = nowUTC(),
            begrunnelse = "Begrunnelse",
            stansdato = nowUTC().toLocalDate(),
            document = emptyList(),
            journalpostId = null,
        )
        val savedVurdering = vurderingRepository.saveManglendeMedvirkningVurdering(vurdering, pdf)
        assertInstanceOf<Vurdering.Stans>(savedVurdering)
        assertEquals(vurdering.personident, savedVurdering.personident)
        assertEquals(vurdering.veilederident, savedVurdering.veilederident)
        assertEquals(vurdering.begrunnelse, savedVurdering.begrunnelse)
        assertEquals(vurdering.stansdato, savedVurdering.stansdato)
        assertEquals(vurdering.document, savedVurdering.document)
        assertEquals(vurdering.journalpostId, savedVurdering.journalpostId)
    }

    @Test
    fun `saves a vurdering with a varsel`() {
        val now = nowUTC()
        val vurdering = Vurdering.Forhandsvarsel(
            uuid = UUID.randomUUID(),
            personident = ARBEIDSTAKER_PERSONIDENT,
            veilederident = VEILEDER_IDENT,
            createdAt = now,
            begrunnelse = "Begrunnelse",
            document = emptyList(),
            journalpostId = null,
            varsel = Varsel(
                UUID.randomUUID(),
                now,
                now.toLocalDate(),
            ),
        )
        val savedVurdering = vurderingRepository.saveManglendeMedvirkningVurdering(vurdering, pdf)
        assertInstanceOf<Vurdering.Forhandsvarsel>(savedVurdering)
        assertEquals(vurdering.personident, savedVurdering.personident)
        assertEquals(vurdering.veilederident, savedVurdering.veilederident)
        assertEquals(vurdering.begrunnelse, savedVurdering.begrunnelse)
        assertEquals(vurdering.document, savedVurdering.document)
        assertEquals(vurdering.journalpostId, savedVurdering.journalpostId)
        assertEquals(vurdering.varsel.uuid, savedVurdering.varsel.uuid)
        assertEquals(vurdering.varsel.svarfrist, savedVurdering.varsel.svarfrist)
    }
}
