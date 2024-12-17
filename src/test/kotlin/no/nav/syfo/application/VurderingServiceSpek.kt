package no.nav.syfo.application

import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_PDL_FAILS
import no.nav.syfo.UserConstants.PDF_FORHANDSVARSEL
import no.nav.syfo.UserConstants.PDF_STANS
import no.nav.syfo.UserConstants.PDF_VURDERING
import no.nav.syfo.UserConstants.VEILEDER_IDENT
import no.nav.syfo.application.model.NewVurderingRequestDTO
import no.nav.syfo.domain.JournalpostId
import no.nav.syfo.domain.Vurdering.Forhandsvarsel
import no.nav.syfo.domain.VurderingType
import no.nav.syfo.generator.generateVurdering
import no.nav.syfo.infrastructure.clients.pdfgen.VurderingPdfService
import no.nav.syfo.infrastructure.database.dropData
import no.nav.syfo.infrastructure.database.getVurdering
import no.nav.syfo.infrastructure.database.getVurderingPdf
import no.nav.syfo.infrastructure.database.repository.VurderingRepository
import no.nav.syfo.infrastructure.journalforing.JournalforingService
import no.nav.syfo.infrastructure.kafka.VurderingProducer
import no.nav.syfo.infrastructure.kafka.VurderingRecord
import no.nav.syfo.infrastructure.mock.mockedJournalpostId
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldNotBeNull
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate
import java.util.concurrent.Future
import kotlin.test.fail

class VurderingServiceSpek : Spek({
    describe(VurderingService::class.java.simpleName) {

        val externalMockEnvironment = ExternalMockEnvironment.instance
        val database = externalMockEnvironment.database

        val vurderingRepository = VurderingRepository(database = database)
        val journalforingService = JournalforingService(
            dokarkivClient = externalMockEnvironment.dokarkivClient,
            pdlClient = externalMockEnvironment.pdlClient,
            isJournalforingRetryEnabled = externalMockEnvironment.environment.isJournalforingRetryEnabled,
        )

        val mockVurderingProducer = mockk<KafkaProducer<String, VurderingRecord>>(relaxed = true)
        val vurderingProducer = VurderingProducer(
            producer = mockVurderingProducer,
        )
        val vurderingPdfService = VurderingPdfService(
            externalMockEnvironment.pdfgenClient,
            externalMockEnvironment.pdlClient,
        )

        val vurderingService = VurderingService(
            journalforingService = journalforingService,
            vurderingRepository = vurderingRepository,
            vurderingProducer = vurderingProducer,
            vurderingPdfService = vurderingPdfService,
        )

        afterEachTest {
            database.dropData()
        }

        val vurderingForhandsvarsel = generateVurdering(type = VurderingType.FORHANDSVARSEL)
        val vurderingOppfylt = generateVurdering(type = VurderingType.OPPFYLT)
        val vurderingStans = generateVurdering(type = VurderingType.STANS)
        val vurderingIkkeAktuell = generateVurdering(type = VurderingType.IKKE_AKTUELL)

        describe("Create new vurdering") {
            it("Lagrer vurdering") {
                val savedVurdering = runBlocking {
                    vurderingService.createNewVurdering(
                        veilederident = VEILEDER_IDENT,
                        newVurdering = NewVurderingRequestDTO.Forhandsvarsel(
                            personident = ARBEIDSTAKER_PERSONIDENT.value,
                            begrunnelse = "Begrunnelse",
                            document = emptyList(),
                            varselSvarfrist = LocalDate.now().plusDays(14),
                        ),
                        callId = "callId",
                    )
                }

                savedVurdering.personident shouldBeEqualTo ARBEIDSTAKER_PERSONIDENT
                savedVurdering.veilederident shouldBeEqualTo VEILEDER_IDENT
                savedVurdering.vurderingType shouldBeEqualTo VurderingType.FORHANDSVARSEL
                savedVurdering.begrunnelse shouldBeEqualTo "Begrunnelse"
                savedVurdering.document shouldBeEqualTo emptyList()
                savedVurdering shouldBeInstanceOf Forhandsvarsel::class
                when (savedVurdering) {
                    is Forhandsvarsel -> savedVurdering.varsel.svarfrist shouldBeEqualTo LocalDate.now().plusDays(14)
                    else -> fail("Expected savedVurdering to be an instance of Forhandsvarsel")
                }

                val pVurderingPdf = database.getVurderingPdf(savedVurdering.uuid)
                pVurderingPdf.shouldNotBeNull()
            }

            it("Publiserer lagret vurdering på kafka") {
                coEvery { mockVurderingProducer.send(any()) } returns mockk<Future<RecordMetadata>>(relaxed = true)

                val savedVurdering = runBlocking {
                    vurderingService.createNewVurdering(
                        veilederident = VEILEDER_IDENT,
                        newVurdering = NewVurderingRequestDTO.Forhandsvarsel(
                            personident = ARBEIDSTAKER_PERSONIDENT.value,
                            begrunnelse = "Begrunnelse",
                            document = emptyList(),
                            varselSvarfrist = LocalDate.now().plusDays(14),
                        ),
                        callId = "callId",
                    )
                }

                val producerRecordSlot = slot<ProducerRecord<String, VurderingRecord>>()
                verifyOrder {
                    mockVurderingProducer.send(capture(producerRecordSlot))
                }

                val vurderingRecord = producerRecordSlot.captured.value()

                vurderingRecord.uuid shouldBeEqualTo savedVurdering.uuid
                vurderingRecord.personident shouldBeEqualTo savedVurdering.personident
                vurderingRecord.veilederident shouldBeEqualTo savedVurdering.veilederident
                vurderingRecord.createdAt shouldBeEqualTo savedVurdering.createdAt
                vurderingRecord.begrunnelse shouldBeEqualTo savedVurdering.begrunnelse
                vurderingRecord.vurderingType.value shouldBeEqualTo savedVurdering.vurderingType
                vurderingRecord.vurderingType.isActive shouldBeEqualTo savedVurdering.vurderingType.isActive
                when (savedVurdering) {
                    is Forhandsvarsel -> vurderingRecord.varsel?.svarfrist shouldBeEqualTo savedVurdering.varsel.svarfrist
                    else -> fail("Expected published record svarfrist to equal saved vurdering svarfrist")
                }
            }
        }

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

            it("journalfører STANS vurdering") {
                vurderingRepository.saveManglendeMedvirkningVurdering(
                    vurdering = vurderingStans,
                    vurderingPdf = PDF_STANS,
                )

                val journalforteVurderinger = runBlocking {
                    vurderingService.journalforVurderinger()
                }

                val (success, failed) = journalforteVurderinger.partition { it.isSuccess }
                failed.size shouldBeEqualTo 0
                success.size shouldBeEqualTo 1

                val pVurdering = database.getVurdering(vurderingStans.uuid)
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
