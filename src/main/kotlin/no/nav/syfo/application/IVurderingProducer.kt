package no.nav.syfo.application

import no.nav.syfo.domain.Vurdering

interface IVurderingProducer {

    fun publishVurdering(vurdering: Vurdering): Result<Vurdering>
}
