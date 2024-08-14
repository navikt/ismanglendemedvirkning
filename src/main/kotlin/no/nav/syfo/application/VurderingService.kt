package no.nav.syfo.application

import no.nav.syfo.domain.*
import no.nav.syfo.domain.DocumentComponent
import no.nav.syfo.domain.ManglendeMedvirkningVurdering
import no.nav.syfo.domain.Personident
import no.nav.syfo.domain.Veilederident
import no.nav.syfo.domain.VurderingType
import org.slf4j.LoggerFactory
import java.time.LocalDate

class VurderingService(
    private val journalforingService: IJournalforingService,
    private val vurderingRepository: IVurderingRepository,
    private val vurderingProducer: IVurderingProducer,
) {

    fun createNewVurdering(
        personident: Personident,
        veilederident: Veilederident,
        vurderingType: VurderingType,
        begrunnelse: String,
        document: List<DocumentComponent>,
        varselSvarfrist: LocalDate?,
        callId: String,
    ): ManglendeMedvirkningVurdering {
        val newVurdering = ManglendeMedvirkningVurdering.create(
            personident = personident,
            veilederident = veilederident,
            begrunnelse = begrunnelse,
            document = document,
            varselSvarfrist = varselSvarfrist,
            type = vurderingType,
        )

        // TODO: Get vurdering pdf from ispdfgen

        val savedVurdering = vurderingRepository.saveManglendeMedvirkningVurdering(
            vurdering = newVurdering,
            vurderingPdf = byteArrayOf(),
        )

        vurderingProducer.publishVurdering(savedVurdering)
            .map { vurderingRepository.updatePublishedAt(it.uuid) }
            .onFailure { log.error("Failed to publish vurdering with uuid: ${savedVurdering.uuid}, and message: ${it.message}") }
        return savedVurdering
    }

    suspend fun journalforVurderinger(): List<Result<ManglendeMedvirkningVurdering>> {
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

    companion object {
        private val log = LoggerFactory.getLogger(VurderingService::class.java)
    }
}
