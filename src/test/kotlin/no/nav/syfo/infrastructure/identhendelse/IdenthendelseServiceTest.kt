package no.nav.syfo.infrastructure.identhendelse

import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.UserConstants
import no.nav.syfo.domain.VurderingType
import no.nav.syfo.generator.generateIdenthendelse
import no.nav.syfo.generator.generateVurdering
import no.nav.syfo.infrastructure.database.repository.VurderingRepository
import no.nav.syfo.infrastructure.kafka.identhendelse.IdenthendelseService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private val aktivIdent = UserConstants.ARBEIDSTAKER_PERSONIDENT
private val inaktivIdent = UserConstants.ARBEIDSTAKER_PERSONIDENT_NAME_WITH_DASH
private val annenInaktivIdent = UserConstants.ARBEIDSTAKER_PERSONIDENT_NO_NAME

class IdenthendelseServiceTest {
    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val vurderingRepository = VurderingRepository(database = database)
    private val identhendelseService = IdenthendelseService(
        vurderingRepository = vurderingRepository,
    )

    @BeforeEach
    fun beforeEach() {
        database.resetDatabase()
    }

    private val vurderingMedInaktivIdent = generateVurdering(personident = inaktivIdent, type = VurderingType.OPPFYLT)
    private val vurderingMedAnnenInaktivIdent = generateVurdering(personident = annenInaktivIdent, type = VurderingType.STANS)

    @Test
    fun `Flytter vurdering fra inaktiv ident til ny ident når person får ny ident`() {
        vurderingRepository.saveManglendeMedvirkningVurdering(
            vurdering = vurderingMedInaktivIdent,
            vurderingPdf = UserConstants.PDF_VURDERING,
        )

        val identhendelse = generateIdenthendelse(
            aktivIdent = aktivIdent,
            inaktiveIdenter = listOf(inaktivIdent)
        )
        identhendelseService.handle(identhendelse)

        assertTrue(vurderingRepository.getVurderinger(personident = inaktivIdent).isEmpty())
        assertTrue(vurderingRepository.getVurderinger(personident = aktivIdent).isNotEmpty())
    }

    @Test
    fun `Flytter vurderinger fra inaktive identer når person for ny ident`() {
        vurderingRepository.saveManglendeMedvirkningVurdering(
            vurdering = vurderingMedInaktivIdent,
            vurderingPdf = UserConstants.PDF_VURDERING
        )
        vurderingRepository.saveManglendeMedvirkningVurdering(
            vurdering = vurderingMedAnnenInaktivIdent,
            vurderingPdf = UserConstants.PDF_VURDERING
        )

        val identhendelse = generateIdenthendelse(
            aktivIdent = aktivIdent,
            inaktiveIdenter = listOf(inaktivIdent, annenInaktivIdent)
        )
        identhendelseService.handle(identhendelse)

        assertTrue(vurderingRepository.getVurderinger(personident = inaktivIdent).isEmpty())
        assertTrue(vurderingRepository.getVurderinger(personident = annenInaktivIdent).isEmpty())
        assertEquals(2, vurderingRepository.getVurderinger(personident = aktivIdent).size)
    }

    @Test
    fun `Oppdaterer ingenting når person får ny ident og uten vurdering på inaktiv ident`() {
        val identhendelse = generateIdenthendelse(
            aktivIdent = aktivIdent,
            inaktiveIdenter = listOf(inaktivIdent)
        )
        identhendelseService.handle(identhendelse)

        assertTrue(vurderingRepository.getVurderinger(personident = inaktivIdent).isEmpty())
        assertTrue(vurderingRepository.getVurderinger(personident = aktivIdent).isEmpty())
    }

    @Test
    fun `Oppdaterer ingenting når person får ny ident uten inaktiv identer`() {
        vurderingRepository.saveManglendeMedvirkningVurdering(
            vurdering = vurderingMedInaktivIdent,
            vurderingPdf = UserConstants.PDF_VURDERING,
        )

        val identhendelse = generateIdenthendelse(
            aktivIdent = aktivIdent,
            inaktiveIdenter = emptyList()
        )
        identhendelseService.handle(identhendelse)

        assertTrue(vurderingRepository.getVurderinger(personident = inaktivIdent).isNotEmpty())
        assertTrue(vurderingRepository.getVurderinger(personident = aktivIdent).isEmpty())
    }

    @Test
    fun `Oppdaterer ingenting når person mangler aktiv ident`() {
        vurderingRepository.saveManglendeMedvirkningVurdering(
            vurdering = vurderingMedInaktivIdent,
            vurderingPdf = UserConstants.PDF_VURDERING,
        )

        val identhendelse = generateIdenthendelse(
            aktivIdent = null,
            inaktiveIdenter = listOf(inaktivIdent)
        )
        identhendelseService.handle(identhendelse)

        assertTrue(vurderingRepository.getVurderinger(personident = inaktivIdent).isNotEmpty())
        assertTrue(vurderingRepository.getVurderinger(personident = aktivIdent).isEmpty())
    }
}
