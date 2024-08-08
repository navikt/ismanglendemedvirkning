package no.nav.syfo.infrastructure.clients.dokarkiv.dto

enum class BrukerIdType(
    val value: String,
) {
    PERSON_IDENT("FNR"),
}

data class Bruker(
    val id: String,
    val idType: String,
)
