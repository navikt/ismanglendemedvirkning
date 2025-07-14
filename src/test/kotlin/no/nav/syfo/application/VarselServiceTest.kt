package no.nav.syfo.application

import io.mockk.*
import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.UserConstants
import no.nav.syfo.domain.JournalpostId
import no.nav.syfo.domain.Varsel
import no.nav.syfo.domain.Vurdering
import no.nav.syfo.domain.VurderingType
import no.nav.syfo.generator.generateVurdering
import no.nav.syfo.infrastructure.database.repository.VarselRepository
import no.nav.syfo.infrastructure.database.repository.VurderingRepository
import no.nav.syfo.infrastructure.kafka.esyfovarsel.EsyfovarselHendelseProducer
import no.nav.syfo.infrastructure.kafka.esyfovarsel.dto.ArbeidstakerHendelse
import no.nav.syfo.infrastructure.kafka.esyfovarsel.dto.EsyfovarselHendelse
import no.nav.syfo.infrastructure.kafka.esyfovarsel.dto.HendelseType
import no.nav.syfo.infrastructure.kafka.esyfovarsel.dto.VarselData
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.Future

private val journalpostId = JournalpostId("123")

class VarselServiceTest {
    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database

    private val varselRepository = VarselRepository(database = database)
    private val vurderingRepository = VurderingRepository(database = database)
    private val mockEsyfoVarselHendelseProducer = mockk<KafkaProducer<String, EsyfovarselHendelse>>()
    private val arbeidstakerForhandsvarselProducer = EsyfovarselHendelseProducer(producer = mockEsyfoVarselHendelseProducer)
    private val varselProducer = no.nav.syfo.infrastructure.kafka.VarselProducer(
        arbeidstakerForhandsvarselProducer = arbeidstakerForhandsvarselProducer,
    )
    private val varselService = VarselService(
        varselRepository = varselRepository,
        varselProducer = varselProducer,
    )

    @BeforeEach
    fun beforeEach() {
        clearAllMocks()
        coEvery { mockEsyfoVarselHendelseProducer.send(any()) } returns mockk<Future<RecordMetadata>>(relaxed = true)
    }

    @AfterEach
    fun afterEach() {
        database.resetDatabase()
    }

    private val vurderingForhandsvarsel = generateVurdering(
        type = VurderingType.FORHANDSVARSEL,
    ) as Vurdering.Forhandsvarsel

    private fun createUnpublishedVarsel(): Varsel {
        vurderingRepository.saveManglendeMedvirkningVurdering(
            vurdering = vurderingForhandsvarsel,
            vurderingPdf = UserConstants.PDF_FORHANDSVARSEL,
        )
        val unpublishedVarsel = vurderingForhandsvarsel.varsel
        vurderingRepository.setJournalpostId(vurderingForhandsvarsel.copy(journalpostId = journalpostId))

        return unpublishedVarsel
    }

    @Test
    fun `publishes unpublished varsel`() {
        val unpublishedVarsel = createUnpublishedVarsel()

        val (success, failed) = varselService.publishUnpublishedVarsler().partition { it.isSuccess }
        assertEquals(0, failed.size)
        assertEquals(1, success.size)

        val producerRecordSlot = slot<ProducerRecord<String, EsyfovarselHendelse>>()
        verify(exactly = 1) { mockEsyfoVarselHendelseProducer.send(capture(producerRecordSlot)) }

        val publishedVarsel = success.first().getOrThrow()
        assertEquals(unpublishedVarsel.uuid, publishedVarsel.uuid)

        assertTrue(varselRepository.getUnpublishedVarsler().isEmpty())

        val esyfovarselHendelse = producerRecordSlot.captured.value() as ArbeidstakerHendelse
        assertEquals(HendelseType.SM_FORHANDSVARSEL_MANGLENDE_MEDVIRKNING, esyfovarselHendelse.type)
        assertEquals(UserConstants.ARBEIDSTAKER_PERSONIDENT.value, esyfovarselHendelse.arbeidstakerFnr)
        val varselData = esyfovarselHendelse.data as VarselData
        assertEquals(publishedVarsel.uuid.toString(), varselData.journalpost?.uuid)
        assertEquals(journalpostId.value, varselData.journalpost?.id)
    }

    @Test
    fun `publishes nothing when no unpublished varsel`() {
        val (success, failed) = varselService.publishUnpublishedVarsler().partition { it.isSuccess }
        assertEquals(0, failed.size)
        assertEquals(0, success.size)

        verify(exactly = 0) { mockEsyfoVarselHendelseProducer.send(any()) }
    }

    @Test
    fun `fails publishing when kafka-producer fails`() {
        val unpublishedVarsel = createUnpublishedVarsel()
        every { mockEsyfoVarselHendelseProducer.send(any()) } throws Exception("Error producing to kafka")

        val (success, failed) = varselService.publishUnpublishedVarsler().partition { it.isSuccess }
        assertEquals(1, failed.size)
        assertEquals(0, success.size)

        verify(exactly = 1) { mockEsyfoVarselHendelseProducer.send(any()) }

        val (_, _, varsel) = varselRepository.getUnpublishedVarsler().first()
        assertEquals(unpublishedVarsel.uuid, varsel.uuid)
    }
}
