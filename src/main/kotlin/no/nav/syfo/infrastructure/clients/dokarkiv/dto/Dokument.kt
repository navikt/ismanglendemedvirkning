package no.nav.syfo.infrastructure.clients.dokarkiv.dto

enum class BrevkodeType(
    val value: String,
) {
    MANGLENDE_MEDVIRKNING_FORHANDSVARSEL("OPPF_MANGLENDE_MEDVIRKNING_FORHANDSVARSEL"),
    MANGLENDE_MEDVIRKNING_VURDERING("OPPF_MANGLENDE_MEDVIRKNING_VURDERING"),
    MANGLENDE_MEDVIRKNING_STANS("OPPF_MANGLENDE_MEDVIRKNING_STANS"),
}

data class Dokument(
    val brevkode: String,
    val dokumentvarianter: List<Dokumentvariant>,
    val tittel: String? = null,
)
