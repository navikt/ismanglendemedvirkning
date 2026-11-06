package no.nav.syfo.application

import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.UserConstants
import no.nav.syfo.domain.JournalpostId
import no.nav.syfo.domain.Varsel
import no.nav.syfo.domain.Vurdering
import no.nav.syfo.domain.VurderingType
import no.nav.syfo.generator.generateVurdering
import no.nav.syfo.infrastructure.database.repository.VarselRepository
import no.nav.syfo.infrastructure.database.repository.VurderingRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private val journalpostId = JournalpostId("123")

class VarselServiceTest {
    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database

    private val varselRepository = VarselRepository(database = database)
    private val vurderingRepository = VurderingRepository(database = database)
    private val dokdistClient = mockk<IDokdistClient>()
    private val varselService = VarselService(
        varselRepository = varselRepository,
        dokdistClient = dokdistClient,
    )

    @BeforeEach
    fun beforeEach() {
        clearAllMocks()
        coEvery { dokdistClient.distribuer(journalpostId) } returns "bestillingsId"
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
    fun `distributes unpublished varsel only once`() = runBlocking {
        val unpublishedVarsel = createUnpublishedVarsel()

        val (success, failed) = varselService.publishUnpublishedVarsler().partition { it.isSuccess }
        assertEquals(0, failed.size)
        assertEquals(1, success.size)

        coVerify(exactly = 1) { dokdistClient.distribuer(journalpostId) }
        assertEquals(unpublishedVarsel.uuid, success.first().getOrThrow().uuid)
        assertTrue(varselRepository.getUnpublishedVarsler().isEmpty())

        assertTrue(varselService.publishUnpublishedVarsler().isEmpty())
        coVerify(exactly = 1) { dokdistClient.distribuer(journalpostId) }
    }

    @Test
    fun `distributes nothing when no unpublished varsel`() = runBlocking {
        assertTrue(varselService.publishUnpublishedVarsler().isEmpty())

        coVerify(exactly = 0) { dokdistClient.distribuer(journalpostId) }
    }

    @Test
    fun `retries distribution when dokdist fails`() = runBlocking {
        val unpublishedVarsel = createUnpublishedVarsel()
        coEvery { dokdistClient.distribuer(journalpostId) } throws Exception("Error calling dokdist")

        val (success, failed) = varselService.publishUnpublishedVarsler().partition { it.isSuccess }
        assertEquals(1, failed.size)
        assertEquals(0, success.size)

        val (_, _, varsel) = varselRepository.getUnpublishedVarsler().first()
        assertEquals(unpublishedVarsel.uuid, varsel.uuid)

        coEvery { dokdistClient.distribuer(journalpostId) } returns "bestillingsId"
        assertTrue(varselService.publishUnpublishedVarsler().single().isSuccess)
        assertTrue(varselRepository.getUnpublishedVarsler().isEmpty())
    }
}
