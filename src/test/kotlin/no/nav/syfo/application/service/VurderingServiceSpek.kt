package no.nav.syfo.application.service

import kotlinx.coroutines.runBlocking
import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_PDL_FAILS
import no.nav.syfo.UserConstants.PDF_STANS
import no.nav.syfo.UserConstants.PDF_FORHANDSVARSEL
import no.nav.syfo.UserConstants.PDF_VURDERING
import no.nav.syfo.application.*
import no.nav.syfo.domain.JournalpostId
import no.nav.syfo.domain.VurderingType
import no.nav.syfo.generator.generateVurdering
import no.nav.syfo.infrastructure.database.dropData
import no.nav.syfo.infrastructure.database.getVurdering
import no.nav.syfo.infrastructure.database.repository.VurderingRepository
import no.nav.syfo.infrastructure.journalforing.JournalforingService
import no.nav.syfo.infrastructure.mock.mockedJournalpostId
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class VurderingServiceSpek : Spek({
    describe(VurderingService::class.java.simpleName) {

        val externalMockEnvironment = ExternalMockEnvironment.instance
        val database = externalMockEnvironment.database

        val vurderingRepository = VurderingRepository(database = database)
        val journalforingService = JournalforingService(
            dokarkivClient = externalMockEnvironment.dokarkivClient,
            pdlClient = externalMockEnvironment.pdlClient,
        )

        val vurderingService = VurderingService(
            vurderingRepository = vurderingRepository,
            journalforingService = journalforingService,
        )

        afterEachTest {
            database.dropData()
        }

        val vurderingForhandsvarsel = generateVurdering(type = VurderingType.FORHANDSVARSEL)
        val vurderingOppfylt = generateVurdering(type = VurderingType.OPPFYLT)
        val vurderingAvslag = generateVurdering(type = VurderingType.STANS)
        val vurderingIkkeAktuell = generateVurdering(type = VurderingType.IKKE_AKTUELL)

        describe("Journalføring") {
            it("journalfører FORHANDSVARSEL vurdering") {
                vurderingRepository.saveManglendeMedvirkningVurdering(
                    vurderingPdf = PDF_FORHANDSVARSEL,
                    vurdering = vurderingForhandsvarsel,
                )

                val journalforteVurderinger = runBlocking {
                    vurderingService.journalforVurderinger()
                }

                val (success, failed) = journalforteVurderinger.partition { it.isSuccess }
                failed.size shouldBeEqualTo 0
                success.size shouldBeEqualTo 1

                val journalfortVurdering = success.first().getOrThrow()
                journalfortVurdering.journalpostId?.value shouldBeEqualTo mockedJournalpostId.toString()

                val pVurdering = database.getVurdering(journalfortVurdering.uuid)
                pVurdering!!.updatedAt shouldBeGreaterThan pVurdering.createdAt
                pVurdering.type shouldBeEqualTo VurderingType.FORHANDSVARSEL
                pVurdering.journalpostId?.value shouldBeEqualTo mockedJournalpostId.toString()
            }

            it("journalfører OPPFYLT vurdering") {
                vurderingRepository.saveManglendeMedvirkningVurdering(
                    vurdering = vurderingOppfylt,
                    vurderingPdf = PDF_VURDERING,
                )

                val journalforteVurderinger = runBlocking {
                    vurderingService.journalforVurderinger()
                }

                val (success, failed) = journalforteVurderinger.partition { it.isSuccess }
                failed.size shouldBeEqualTo 0
                success.size shouldBeEqualTo 1

                val journalfortVurdering = success.first().getOrThrow()
                journalfortVurdering.journalpostId?.value shouldBeEqualTo mockedJournalpostId.toString()

                val pVurdering = database.getVurdering(journalfortVurdering.uuid)
                pVurdering!!.updatedAt shouldBeGreaterThan pVurdering.createdAt
                pVurdering.type shouldBeEqualTo VurderingType.OPPFYLT
                pVurdering.journalpostId?.value shouldBeEqualTo mockedJournalpostId.toString()
            }

            it("journalfører AVSLAG vurdering") {
                vurderingRepository.saveManglendeMedvirkningVurdering(
                    vurdering = vurderingAvslag,
                    vurderingPdf = PDF_STANS,
                )

                val journalforteVurderinger = runBlocking {
                    vurderingService.journalforVurderinger()
                }

                val (success, failed) = journalforteVurderinger.partition { it.isSuccess }
                failed.size shouldBeEqualTo 0
                success.size shouldBeEqualTo 1

                val pVurdering = database.getVurdering(vurderingAvslag.uuid)
                pVurdering!!.type shouldBeEqualTo VurderingType.STANS
                pVurdering.journalpostId?.value shouldBeEqualTo mockedJournalpostId.toString()
            }

            it("journalfører IKKE_AKTUELL vurdering") {
                vurderingRepository.saveManglendeMedvirkningVurdering(
                    vurdering = vurderingIkkeAktuell,
                    vurderingPdf = PDF_VURDERING,
                )

                val journalforteVurderinger = runBlocking {
                    vurderingService.journalforVurderinger()
                }

                val (success, failed) = journalforteVurderinger.partition { it.isSuccess }
                failed.size shouldBeEqualTo 0
                success.size shouldBeEqualTo 1

                val journalfortVurdering = success.first().getOrThrow()
                journalfortVurdering.journalpostId?.value shouldBeEqualTo mockedJournalpostId.toString()

                val pVurdering = database.getVurdering(journalfortVurdering.uuid)
                pVurdering!!.updatedAt shouldBeGreaterThan pVurdering.createdAt
                pVurdering.type shouldBeEqualTo VurderingType.IKKE_AKTUELL
                pVurdering.journalpostId?.value shouldBeEqualTo mockedJournalpostId.toString()
            }

            it("journalfører ikke når ingen vurderinger") {
                val journalforteVurderinger = runBlocking {
                    vurderingService.journalforVurderinger()
                }

                val (success, failed) = journalforteVurderinger.partition { it.isSuccess }
                failed.size shouldBeEqualTo 0
                success.size shouldBeEqualTo 0
            }

            it("journalfører ikke når vurdering allerede er journalført") {
                vurderingRepository.saveManglendeMedvirkningVurdering(
                    vurderingPdf = PDF_FORHANDSVARSEL,
                    vurdering = vurderingForhandsvarsel,
                )
                val journalfortVurdering = vurderingForhandsvarsel.journalfor(journalpostId = JournalpostId(mockedJournalpostId.toString()))
                vurderingRepository.setJournalpostId(journalfortVurdering)

                val journalforteVurderinger = runBlocking {
                    vurderingService.journalforVurderinger()
                }

                val (success, failed) = journalforteVurderinger.partition { it.isSuccess }
                failed.size shouldBeEqualTo 0
                success.size shouldBeEqualTo 0
            }

            it("journalfører vurderinger selv om noen feiler") {
                val vurderingFails = generateVurdering(
                    type = VurderingType.FORHANDSVARSEL,
                    personident = ARBEIDSTAKER_PERSONIDENT_PDL_FAILS,
                )
                vurderingRepository.saveManglendeMedvirkningVurdering(
                    vurderingPdf = PDF_FORHANDSVARSEL,
                    vurdering = vurderingForhandsvarsel,
                )
                vurderingRepository.saveManglendeMedvirkningVurdering(
                    vurderingPdf = PDF_FORHANDSVARSEL,
                    vurdering = vurderingFails,
                )

                val journalforteVurderinger = runBlocking {
                    vurderingService.journalforVurderinger()
                }

                val (success, failed) = journalforteVurderinger.partition { it.isSuccess }
                failed.size shouldBeEqualTo 1
                success.size shouldBeEqualTo 1
            }

            it("journalfører flere vurderinger av ulik type") {
                vurderingRepository.saveManglendeMedvirkningVurdering(
                    vurderingPdf = PDF_FORHANDSVARSEL,
                    vurdering = vurderingForhandsvarsel,
                )
                vurderingRepository.saveManglendeMedvirkningVurdering(
                    vurderingPdf = PDF_VURDERING,
                    vurdering = vurderingOppfylt,
                )

                val journalforteVurderinger = runBlocking {
                    vurderingService.journalforVurderinger()
                }

                val (success, failed) = journalforteVurderinger.partition { it.isSuccess }
                failed.size shouldBeEqualTo 0
                success.size shouldBeEqualTo 2
            }
        }
    }
})
