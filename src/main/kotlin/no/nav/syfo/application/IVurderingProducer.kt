package no.nav.syfo.application

import no.nav.syfo.domain.ManglendeMedvirkningVurdering

interface IVurderingProducer {

    fun publishVurdering(vurdering: ManglendeMedvirkningVurdering): Result<ManglendeMedvirkningVurdering>
}
