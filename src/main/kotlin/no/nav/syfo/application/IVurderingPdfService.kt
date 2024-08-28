package no.nav.syfo.application

import no.nav.syfo.domain.ManglendeMedvirkningVurdering

interface IVurderingPdfService {
    suspend fun createVurderingPdf(
        vurdering: ManglendeMedvirkningVurdering,
        callId: String,
    ): ByteArray
}
