package no.nav.syfo.application

import no.nav.syfo.domain.ManglendeMedvirkningVurdering
import no.nav.syfo.domain.Personident
import no.nav.syfo.domain.Veilederident
import no.nav.syfo.domain.VurderingType
import no.nav.syfo.domain.DocumentComponent
import no.nav.syfo.infrastructure.database.repository.VurderingRepository
import java.time.LocalDate

class VurderingService(
    private val vurderingRepository: VurderingRepository,
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

        // TODO: Journalfor vurdering
        // TODO: Publish new vurdering

        return savedVurdering
    }
}
