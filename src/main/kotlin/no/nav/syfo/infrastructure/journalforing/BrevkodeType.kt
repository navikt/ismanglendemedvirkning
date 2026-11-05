package no.nav.syfo.infrastructure.journalforing

import no.nav.syfo.common.journalforing.Brevkode
import no.nav.syfo.domain.VurderingType

enum class BrevkodeType(
    override val value: String,
) : Brevkode {
    MANGLENDE_MEDVIRKNING_FORHANDSVARSEL("OPPF_MANGLENDE_MEDVIRKNING_FORHANDSVARSEL"),
    MANGLENDE_MEDVIRKNING_VURDERING("OPPF_MANGLENDE_MEDVIRKNING_VURDERING"),
    MANGLENDE_MEDVIRKNING_STANS("OPPF_MANGLENDE_MEDVIRKNING_STANS");

    companion object {
        fun fromVurderingType(vurderingType: VurderingType): BrevkodeType =
            when (vurderingType) {
                VurderingType.FORHANDSVARSEL -> MANGLENDE_MEDVIRKNING_FORHANDSVARSEL
                VurderingType.OPPFYLT, VurderingType.IKKE_AKTUELL, VurderingType.UNNTAK -> MANGLENDE_MEDVIRKNING_VURDERING
                VurderingType.STANS -> MANGLENDE_MEDVIRKNING_STANS
            }
    }
}
