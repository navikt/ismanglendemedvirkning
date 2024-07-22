package no.nav.syfo.application

import no.nav.syfo.domain.ManglendeMedvirkningVurdering

interface IManglendeMedvirkningRepository {
    fun saveManglendeMedvirkningVurdering(
        manglendeMedvirkning: ManglendeMedvirkningVurdering,
        vurderingPdf: ByteArray,
    ): ManglendeMedvirkningVurdering?
}
