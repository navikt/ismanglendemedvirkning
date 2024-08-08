package no.nav.syfo.domain

import no.nav.syfo.infrastructure.clients.dokarkiv.dto.BrevkodeType
import no.nav.syfo.infrastructure.clients.dokarkiv.dto.JournalpostType
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

interface IManglendeMedvirkningsVurdering {
    val uuid: UUID
    val personident: Personident
    val veilederident: String
    val createdAt: OffsetDateTime
    val begrunnelse: String
    val document: List<DocumentComponentDTO>
    val journalpostId: JournalpostId?
}

sealed class ManglendeMedvirkningVurdering(val status: Status) : IManglendeMedvirkningsVurdering {

    data class Forhandsvarsel(
        override val uuid: UUID,
        override val personident: Personident,
        override val veilederident: String,
        override val createdAt: OffsetDateTime,
        override val begrunnelse: String,
        override val document: List<DocumentComponentDTO>,
        override val journalpostId: JournalpostId?,
        val varsel: Varsel,
    ) : ManglendeMedvirkningVurdering(Status.FORHANDSVARSEL)

    data class Oppfylt(
        override val uuid: UUID,
        override val personident: Personident,
        override val veilederident: String,
        override val createdAt: OffsetDateTime,
        override val begrunnelse: String,
        override val document: List<DocumentComponentDTO>,
        override val journalpostId: JournalpostId?,
    ) : ManglendeMedvirkningVurdering(Status.OPPFYLT)

    data class Stans(
        override val uuid: UUID,
        override val personident: Personident,
        override val veilederident: String,
        override val createdAt: OffsetDateTime,
        override val begrunnelse: String,
        override val document: List<DocumentComponentDTO>,
        override val journalpostId: JournalpostId?,
    ) : ManglendeMedvirkningVurdering(Status.STANS)

    data class IkkeAktuell(
        override val uuid: UUID,
        override val personident: Personident,
        override val veilederident: String,
        override val createdAt: OffsetDateTime,
        override val begrunnelse: String,
        override val document: List<DocumentComponentDTO>,
        override val journalpostId: JournalpostId?,
    ) : ManglendeMedvirkningVurdering(Status.IKKE_AKTUELL)
}

enum class Status {
    FORHANDSVARSEL,
    OPPFYLT,
    STANS,
    IKKE_AKTUELL,
}

data class Varsel(
    val uuid: UUID,
    val createdAt: OffsetDateTime,
    val svarfrist: LocalDate,
)

fun Status.getDokumentTittel(): String = when (this) {
    Status.FORHANDSVARSEL -> "Forhåndsvarsel om stans av sykepenger"
    Status.OPPFYLT, Status.IKKE_AKTUELL -> "Vurdering av § 8-8 manglende medvirkning"
    Status.STANS -> "Innstilling om stans"
}

fun Status.getBrevkode(): BrevkodeType = when (this) {
    Status.FORHANDSVARSEL -> BrevkodeType.MANGLENDE_MEDVIRKNING_FORHANDSVARSEL
    Status.OPPFYLT, Status.IKKE_AKTUELL -> BrevkodeType.MANGLENDE_MEDVIRKNING_VURDERING
    Status.STANS -> BrevkodeType.MANGLENDE_MEDVIRKNING_STANS
}

fun Status.getJournalpostType(): JournalpostType = when (this) {
    Status.FORHANDSVARSEL, Status.OPPFYLT, Status.IKKE_AKTUELL -> JournalpostType.UTGAAENDE
    Status.STANS -> JournalpostType.NOTAT
}
