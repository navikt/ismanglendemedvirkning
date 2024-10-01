package no.nav.syfo.infrastructure.clients.dokarkiv.dto

import no.nav.syfo.domain.VurderingType

enum class BrevkodeType(
    val value: String,
) {
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

data class Dokument(
    val brevkode: String,
    val dokumentvarianter: List<Dokumentvariant>,
    val tittel: String? = null,
)
