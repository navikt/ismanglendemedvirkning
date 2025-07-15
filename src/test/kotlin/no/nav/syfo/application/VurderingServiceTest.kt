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
import no.nav.syfo.domain.DocumentComponent
import no.nav.syfo.domain.JournalpostId
import no.nav.syfo.domain.Vurdering
import no.nav.syfo.domain.Vurdering.Forhandsvarsel
import no.nav.syfo.domain.VurderingType
import no.nav.syfo.generator.generateVurdering
import no.nav.syfo.infrastructure.clients.pdfgen.VurderingPdfService
import no.nav.syfo.infrastructure.database.getVurdering
import no.nav.syfo.infrastructure.database.getVurderingPdf
import no.nav.syfo.infrastructure.database.repository.VurderingRepository
import no.nav.syfo.infrastructure.journalforing.JournalforingService
import no.nav.syfo.infrastructure.kafka.VurderingProducer
import no.nav.syfo.infrastructure.kafka.VurderingRecord
import no.nav.syfo.infrastructure.mock.mockedJournalpostId
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.assertNotNull
import java.time.LocalDate
import java.util.concurrent.Future

class VurderingServiceTest {
    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database

    private val vurderingRepository = VurderingRepository(database = database)
    private val journalforingService = JournalforingService(
        dokarkivClient = externalMockEnvironment.dokarkivClient,
        pdlClient = externalMockEnvironment.pdlClient,
        isJournalforingRetryEnabled = externalMockEnvironment.environment.isJournalforingRetryEnabled,
    )

    private val mockVurderingProducer = mockk<KafkaProducer<String, VurderingRecord>>(relaxed = true)
    private val vurderingProducer = VurderingProducer(
        producer = mockVurderingProducer,
    )
    private val vurderingPdfService = VurderingPdfService(
        externalMockEnvironment.pdfgenClient,
        externalMockEnvironment.pdlClient,
    )

    private val vurderingService = VurderingService(
        journalforingService = journalforingService,
        vurderingRepository = vurderingRepository,
        vurderingProducer = vurderingProducer,
        vurderingPdfService = vurderingPdfService,
    )

    @AfterEach
    fun afterEach() {
        database.resetDatabase()
    }

    private val vurderingForhandsvarsel = generateVurdering(type = VurderingType.FORHANDSVARSEL)
    private val vurderingOppfylt = generateVurdering(type = VurderingType.OPPFYLT)
    private val vurderingStans = generateVurdering(type = VurderingType.STANS)
    private val vurderingIkkeAktuell = generateVurdering(type = VurderingType.IKKE_AKTUELL)

    @Nested
    @DisplayName("Create new vurdering")
    inner class CreateNewVurdering {

        @Test
        fun `Lagrer vurdering`() {
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

            assertEquals(ARBEIDSTAKER_PERSONIDENT, savedVurdering.personident)
            assertEquals(VEILEDER_IDENT, savedVurdering.veilederident)
            assertEquals(VurderingType.FORHANDSVARSEL, savedVurdering.vurderingType)
            assertEquals("Begrunnelse", savedVurdering.begrunnelse)
            assertEquals(emptyList<DocumentComponent>(), savedVurdering.document)
            assertInstanceOf<Forhandsvarsel>(savedVurdering)
            assertEquals(LocalDate.now().plusDays(14), savedVurdering.varsel.svarfrist)
            assertNotNull(database.getVurderingPdf(savedVurdering.uuid))
        }

        @Test
        fun `Publiserer lagret vurdering på kafka`() {
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

            assertEquals(savedVurdering.uuid, vurderingRecord.uuid)
            assertEquals(savedVurdering.personident, vurderingRecord.personident)
            assertEquals(savedVurdering.veilederident, vurderingRecord.veilederident)
            assertEquals(savedVurdering.createdAt, vurderingRecord.createdAt)
            assertEquals(savedVurdering.begrunnelse, vurderingRecord.begrunnelse)
            assertEquals(savedVurdering.vurderingType, vurderingRecord.vurderingType.value)
            assertEquals(savedVurdering.vurderingType.isActive, vurderingRecord.vurderingType.isActive)
            assertInstanceOf<Forhandsvarsel>(savedVurdering)
            assertEquals(savedVurdering.varsel.svarfrist, vurderingRecord.varsel?.svarfrist)
        }

        @Test
        fun `Publiserer stans-vurdering med stansdato på kafka`() {
            coEvery { mockVurderingProducer.send(any()) } returns mockk<Future<RecordMetadata>>(relaxed = true)

            val stansVurdering = runBlocking {
                vurderingService.createNewVurdering(
                    veilederident = VEILEDER_IDENT,
                    newVurdering = NewVurderingRequestDTO.Stans(
                        personident = ARBEIDSTAKER_PERSONIDENT.value,
                        begrunnelse = "Begrunnelse",
                        document = emptyList(),
                        stansdato = LocalDate.now().plusDays(14),
                    ),
                    callId = "callId",
                )
            }

            val producerRecordSlot = slot<ProducerRecord<String, VurderingRecord>>()
            verifyOrder {
                mockVurderingProducer.send(capture(producerRecordSlot))
            }

            val vurderingRecord = producerRecordSlot.captured.value()

            assertEquals(stansVurdering.uuid, vurderingRecord.uuid)
            assertInstanceOf<Vurdering.Stans>(stansVurdering)
            assertEquals(stansVurdering.stansdato, vurderingRecord.stansDato)
        }
    }

    @Nested
    @DisplayName("Journalføring")
    inner class Journalforing {

        @Test
        fun `journalfører FORHANDSVARSEL vurdering`() {
            vurderingRepository.saveManglendeMedvirkningVurdering(
                vurderingPdf = PDF_FORHANDSVARSEL,
                vurdering = vurderingForhandsvarsel,
            )

            val journalforteVurderinger = runBlocking {
                vurderingService.journalforVurderinger()
            }

            val (success, failed) = journalforteVurderinger.partition { it.isSuccess }
            assertEquals(0, failed.size)
            assertEquals(1, success.size)

            val journalfortVurdering = success.first().getOrThrow()
            assertEquals(mockedJournalpostId.toString(), journalfortVurdering.journalpostId?.value)

            val pVurdering = database.getVurdering(journalfortVurdering.uuid)
            assertNotNull(pVurdering)
            assertTrue(pVurdering.updatedAt > pVurdering.createdAt)
            assertEquals(VurderingType.FORHANDSVARSEL, pVurdering.type)
            assertEquals(mockedJournalpostId.toString(), pVurdering.journalpostId?.value)
        }

        @Test
        fun `journalfører OPPFYLT vurdering`() {
            vurderingRepository.saveManglendeMedvirkningVurdering(
                vurdering = vurderingOppfylt,
                vurderingPdf = PDF_VURDERING,
            )

            val journalforteVurderinger = runBlocking {
                vurderingService.journalforVurderinger()
            }

            val (success, failed) = journalforteVurderinger.partition { it.isSuccess }
            assertEquals(0, failed.size)
            assertEquals(1, success.size)

            val journalfortVurdering = success.first().getOrThrow()
            assertEquals(mockedJournalpostId.toString(), journalfortVurdering.journalpostId?.value)

            val pVurdering = database.getVurdering(journalfortVurdering.uuid)
            assertNotNull(pVurdering)
            assertTrue(pVurdering.updatedAt > pVurdering.createdAt)
            assertEquals(VurderingType.OPPFYLT, pVurdering.type)
            assertEquals(mockedJournalpostId.toString(), pVurdering.journalpostId?.value)
        }

        @Test
        fun `journalfører STANS vurdering`() {
            vurderingRepository.saveManglendeMedvirkningVurdering(
                vurdering = vurderingStans,
                vurderingPdf = PDF_STANS,
            )

            val journalforteVurderinger = runBlocking {
                vurderingService.journalforVurderinger()
            }

            val (success, failed) = journalforteVurderinger.partition { it.isSuccess }
            assertEquals(0, failed.size)
            assertEquals(1, success.size)

            val pVurdering = database.getVurdering(vurderingStans.uuid)
            assertNotNull(pVurdering)
            assertEquals(VurderingType.STANS, pVurdering.type)
            assertEquals(mockedJournalpostId.toString(), pVurdering.journalpostId?.value)
        }

        @Test
        fun `journalfører IKKE_AKTUELL vurdering`() {
            vurderingRepository.saveManglendeMedvirkningVurdering(
                vurdering = vurderingIkkeAktuell,
                vurderingPdf = PDF_VURDERING,
            )

            val journalforteVurderinger = runBlocking {
                vurderingService.journalforVurderinger()
            }

            val (success, failed) = journalforteVurderinger.partition { it.isSuccess }
            assertEquals(0, failed.size)
            assertEquals(1, success.size)

            val journalfortVurdering = success.first().getOrThrow()
            assertEquals(mockedJournalpostId.toString(), journalfortVurdering.journalpostId?.value)

            val pVurdering = database.getVurdering(journalfortVurdering.uuid)
            assertNotNull(pVurdering)
            assertTrue(pVurdering.updatedAt > pVurdering.createdAt)
            assertEquals(VurderingType.IKKE_AKTUELL, pVurdering.type)
            assertEquals(mockedJournalpostId.toString(), pVurdering.journalpostId?.value)
        }

        @Test
        fun `journalfører ikke når ingen vurderinger`() {
            val journalforteVurderinger = runBlocking {
                vurderingService.journalforVurderinger()
            }

            val (success, failed) = journalforteVurderinger.partition { it.isSuccess }
            assertEquals(0, failed.size)
            assertEquals(0, success.size)
        }

        @Test
        fun `journalfører ikke når vurdering allerede er journalført`() {
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
            assertEquals(0, failed.size)
            assertEquals(0, success.size)
        }

        @Test
        fun `journalfører vurderinger selv om noen feiler`() {
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
            assertEquals(1, failed.size)
            assertEquals(1, success.size)
        }

        @Test
        fun `journalfører flere vurderinger av ulik type`() {
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
            assertEquals(0, failed.size)
            assertEquals(2, success.size)
        }
    }
}
