package no.nav.syfo.application

import no.nav.syfo.domain.*
import java.time.LocalDate

class VurderingService(
    private val vurderingRepository: IVurderingRepository,
    private val journalforingService: IJournalforingService,
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
            manglendeMedvirkning = newVurdering,
            vurderingPdf = byteArrayOf(),
        )

        // TODO: Publish new vurdering

        return savedVurdering
    }

    fun getVurderinger(
        personident: Personident,
    ): List<ManglendeMedvirkningVurdering> =
        vurderingRepository.getVurderinger(personident)

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
}
