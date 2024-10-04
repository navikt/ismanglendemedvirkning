package no.nav.syfo.infrastructure.database

import io.ktor.server.testing.TestApplicationEngine
import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT
import no.nav.syfo.UserConstants.VEILEDER_IDENT
import no.nav.syfo.domain.Varsel
import no.nav.syfo.domain.Vurdering
import no.nav.syfo.infrastructure.database.repository.VurderingRepository
import no.nav.syfo.util.nowUTC
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.UUID

class VurderingRepositorySpek : Spek({

    describe(VurderingRepositorySpek::class.java.simpleName) {
        with(TestApplicationEngine()) {
            start()
            val externalMockEnvironment = ExternalMockEnvironment.instance
            val database = externalMockEnvironment.database
            val vurderingRepository = VurderingRepository(database = database)
            val pdf = byteArrayOf(0x2E, 100)

            afterEachTest { database.dropData() }

            describe("saveVurdering") {
                it("saves a vurdering") {
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
                    savedVurdering.personident shouldBeEqualTo vurdering.personident
                    savedVurdering.veilederident shouldBeEqualTo vurdering.veilederident
                    savedVurdering.begrunnelse shouldBeEqualTo vurdering.begrunnelse
                    savedVurdering.document shouldBeEqualTo vurdering.document
                    savedVurdering.journalpostId shouldBeEqualTo vurdering.journalpostId
                }

                it("saves a vurdering of type STANS") {
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
                    when (savedVurdering) {
                        is Vurdering.Stans -> {
                            savedVurdering.personident shouldBeEqualTo vurdering.personident
                            savedVurdering.veilederident shouldBeEqualTo vurdering.veilederident
                            savedVurdering.begrunnelse shouldBeEqualTo vurdering.begrunnelse
                            savedVurdering.stansdato shouldBeEqualTo vurdering.stansdato
                            savedVurdering.document shouldBeEqualTo vurdering.document
                            savedVurdering.journalpostId shouldBeEqualTo vurdering.journalpostId
                        }
                        else ->
                            throw IllegalStateException("Expected Vurdering.Stans, got $savedVurdering")
                    }
                }

                it("saves a vurdering with a varsel") {
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
                    when (savedVurdering) {
                        is Vurdering.Forhandsvarsel -> {
                            savedVurdering.personident shouldBeEqualTo vurdering.personident
                            savedVurdering.veilederident shouldBeEqualTo vurdering.veilederident
                            savedVurdering.begrunnelse shouldBeEqualTo vurdering.begrunnelse
                            savedVurdering.document shouldBeEqualTo vurdering.document
                            savedVurdering.journalpostId shouldBeEqualTo vurdering.journalpostId
                            savedVurdering.varsel.uuid shouldBeEqualTo vurdering.varsel.uuid
                            savedVurdering.varsel.svarfrist shouldBeEqualTo vurdering.varsel.svarfrist
                        }
                        else ->
                            throw IllegalStateException("Expected ManglendeMedvirkningVurdering.Forhandsvarsel, got $savedVurdering")
                    }
                }
            }
        }
    }
})
