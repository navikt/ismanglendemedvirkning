package no.nav.syfo.infrastructure.clients.dokarkiv.dto

enum class BrevkodeType(
    val value: String,
) {
    MANGLENDE_MEDVIRKNING_FORHANDSVARSEL("OPPF_MANGLENDE_MEDVIRKNING_FORHANDSVARSEL"),
    MANGLENDE_MEDVIRKNING_VURDERING("OPPF_MANGLENDE_MEDVIRKNING_VURDERING"),
    MANGLENDE_MEDVIRKNING_STANS("OPPF_MANGLENDE_MEDVIRKNING_STANS"),
}

data class Dokument private constructor(
    val brevkode: String,
    val dokumentKategori: String? = null,
    val dokumentvarianter: List<Dokumentvariant>,
    val tittel: String? = null,
) {
    companion object {
        fun create(
            brevkode: BrevkodeType,
            dokumentvarianter: List<Dokumentvariant>,
            tittel: String? = null,
        ) = Dokument(
            brevkode = brevkode.value,
            dokumentvarianter = dokumentvarianter,
            tittel = tittel,
        )
    }
}
