package no.nav.syfo.application

import no.nav.syfo.application.model.NewVurderingRequestDTO
import no.nav.syfo.domain.*
import no.nav.syfo.domain.Personident
import no.nav.syfo.domain.Veilederident
import no.nav.syfo.domain.Vurdering
import org.slf4j.LoggerFactory
import java.lang.IllegalArgumentException

class VurderingService(
    private val journalforingService: IJournalforingService,
    private val vurderingRepository: IVurderingRepository,
    private val vurderingProducer: IVurderingProducer,
    private val vurderingPdfService: IVurderingPdfService,
) {

    suspend fun createNewVurdering(
        veilederident: Veilederident,
        newVurdering: NewVurderingRequestDTO,
        callId: String,
    ): Vurdering {
        val newVurdering = when (newVurdering) {
            is NewVurderingRequestDTO.Forhandsvarsel -> {
                val svarfrist = newVurdering.varselSvarfrist
                if (!Vurdering.Forhandsvarsel.hasValidSvarfrist(svarfrist)) {
                    throw IllegalArgumentException("Forhandsvarsel has invalid svarfrist")
                }
                Vurdering.createForhandsvarsel(
                    personident = Personident(newVurdering.personident),
                    veilederident = veilederident,
                    begrunnelse = newVurdering.begrunnelse,
                    document = newVurdering.document,
                    varselSvarfrist = newVurdering.varselSvarfrist,
                )
            }
            is NewVurderingRequestDTO.Oppfylt ->
                Vurdering.createOppfylt(
                    personident = Personident(newVurdering.personident),
                    veilederident = veilederident,
                    begrunnelse = newVurdering.begrunnelse,
                    document = newVurdering.document,
                )
            is NewVurderingRequestDTO.Stans ->
                Vurdering.createStans(
                    personident = Personident(newVurdering.personident),
                    veilederident = veilederident,
                    begrunnelse = newVurdering.begrunnelse,
                    stansdato = newVurdering.stansdato,
                    document = newVurdering.document,
                )
            is NewVurderingRequestDTO.IkkeAktuell ->
                Vurdering.createIkkeAktuell(
                    personident = Personident(newVurdering.personident),
                    veilederident = veilederident,
                    begrunnelse = newVurdering.begrunnelse,
                    document = newVurdering.document,
                )
            is NewVurderingRequestDTO.Unntak ->
                Vurdering.createUnntak(
                    personident = Personident(newVurdering.personident),
                    veilederident = veilederident,
                    begrunnelse = newVurdering.begrunnelse,
                    document = newVurdering.document,
                )
        }

        val pdf = vurderingPdfService.createVurderingPdf(
            vurdering = newVurdering,
            callId = callId,
        )

        val savedVurdering = vurderingRepository.saveManglendeMedvirkningVurdering(
            vurdering = newVurdering,
            vurderingPdf = pdf,
        )

        vurderingProducer.publishVurdering(savedVurdering)
            .map { vurderingRepository.updatePublishedAt(it.uuid) }
            .onFailure { log.error("Failed to publish vurdering with uuid: ${savedVurdering.uuid}, and message: ${it.message}") }

        return savedVurdering
    }

    fun getVurderinger(
        personident: Personident,
    ): List<Vurdering> =
        vurderingRepository.getVurderinger(personident)

    suspend fun journalforVurderinger(): List<Result<Vurdering>> {
        val notJournalforteVurderinger = vurderingRepository.getNotJournalforteVurderinger()

        return notJournalforteVurderinger.map { (vurdering, pdf) ->
            runCatching {
                val journalpostId = journalforingService.journalfor(
                    personident = vurdering.personident,
                    pdf = pdf,
                    vurdering = vurdering,
                )
                val journalfortVurdering = vurdering.journalfor(
                    journalpostId = JournalpostId(journalpostId.toString()),
                )
                vurderingRepository.setJournalpostId(journalfortVurdering)

                journalfortVurdering
            }
        }
    }

    fun getLatestVurderingForPersoner(
        personidenter: List<Personident>,
    ): Map<Personident, Vurdering> =
        vurderingRepository.getLatestVurderingForPersoner(personidenter)

    companion object {
        val FORHANDSVARSEL_ALLOWED_SVARFRIST_DAYS_SHORTEST = 21L
        val FORHANDSVARSEL_ALLOWED_SVARFRIST_DAYS_LONGEST = 42L

        private val log = LoggerFactory.getLogger(VurderingService::class.java)
    }
}
