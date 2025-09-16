package no.nav.syfo.infrastructure.clients.dokarkiv.dto

import no.nav.syfo.domain.VurderingType

private const val JOURNALFORENDE_ENHET = 9999

enum class JournalpostType {
    UTGAAENDE,
    NOTAT;

    companion object {
        fun fromVurderingType(vurderingType: VurderingType): JournalpostType =
            when (vurderingType) {
                VurderingType.FORHANDSVARSEL, VurderingType.OPPFYLT, VurderingType.IKKE_AKTUELL, VurderingType.UNNTAK -> UTGAAENDE
                VurderingType.STANS -> NOTAT
            }
    }
}

enum class JournalpostTema(val value: String) {
    OPPFOLGING("OPP"),
}

enum class JournalpostKanal(
    val value: String,
) {
    DITT_NAV("NAV_NO"),
}

data class JournalpostRequest(
    val avsenderMottaker: AvsenderMottaker?,
    val tittel: String,
    val bruker: Bruker? = null,
    val dokumenter: List<Dokument>,
    val journalfoerendeEnhet: Int? = JOURNALFORENDE_ENHET,
    val journalpostType: String,
    val tema: String = JournalpostTema.OPPFOLGING.value,
    val sak: Sak = Sak(),
    val eksternReferanseId: String,
    val overstyrInnsynsregler: String? = null,
    val kanal: String = JournalpostKanal.DITT_NAV.value,
)
