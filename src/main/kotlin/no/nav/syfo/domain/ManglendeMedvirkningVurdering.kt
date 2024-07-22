package no.nav.syfo.domain

import no.nav.syfo.aktivitetskrav.api.DocumentComponentDTO
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
