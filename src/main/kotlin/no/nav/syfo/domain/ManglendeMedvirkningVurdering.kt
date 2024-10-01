package no.nav.syfo.domain

import no.nav.syfo.infrastructure.clients.dokarkiv.dto.BrevkodeType
import no.nav.syfo.infrastructure.clients.dokarkiv.dto.JournalpostType
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

interface IManglendeMedvirkningsVurdering {
    val uuid: UUID
    val personident: Personident
    val veilederident: Veilederident
    val createdAt: OffsetDateTime
    val begrunnelse: String
    val document: List<DocumentComponent>
    val journalpostId: JournalpostId?
}

sealed class ManglendeMedvirkningVurdering(val vurderingType: VurderingType) : IManglendeMedvirkningsVurdering {

    data class Forhandsvarsel(
        override val uuid: UUID,
        override val personident: Personident,
        override val veilederident: Veilederident,
        override val createdAt: OffsetDateTime,
        override val begrunnelse: String,
        override val document: List<DocumentComponent>,
        override val journalpostId: JournalpostId?,
        val varsel: Varsel,
    ) : ManglendeMedvirkningVurdering(VurderingType.FORHANDSVARSEL)

    data class Oppfylt(
        override val uuid: UUID,
        override val personident: Personident,
        override val veilederident: Veilederident,
        override val createdAt: OffsetDateTime,
        override val begrunnelse: String,
        override val document: List<DocumentComponent>,
        override val journalpostId: JournalpostId?,
    ) : ManglendeMedvirkningVurdering(VurderingType.OPPFYLT)

    data class Stans(
        override val uuid: UUID,
        override val personident: Personident,
        override val veilederident: Veilederident,
        override val createdAt: OffsetDateTime,
        override val begrunnelse: String,
        override val document: List<DocumentComponent>,
        override val journalpostId: JournalpostId?,
    ) : ManglendeMedvirkningVurdering(VurderingType.STANS)

    data class IkkeAktuell(
        override val uuid: UUID,
        override val personident: Personident,
        override val veilederident: Veilederident,
        override val createdAt: OffsetDateTime,
        override val begrunnelse: String,
        override val document: List<DocumentComponent>,
        override val journalpostId: JournalpostId?,
    ) : ManglendeMedvirkningVurdering(VurderingType.IKKE_AKTUELL)

    data class Unntak(
        override val uuid: UUID,
        override val personident: Personident,
        override val veilederident: Veilederident,
        override val createdAt: OffsetDateTime,
        override val begrunnelse: String,
        override val document: List<DocumentComponent>,
        override val journalpostId: JournalpostId?,
    ) : ManglendeMedvirkningVurdering(VurderingType.UNNTAK)

    fun journalfor(journalpostId: JournalpostId): ManglendeMedvirkningVurdering = when (this) {
        is Forhandsvarsel -> this.copy(journalpostId = journalpostId)
        is Oppfylt -> this.copy(journalpostId = journalpostId)
        is Stans -> this.copy(journalpostId = journalpostId)
        is IkkeAktuell -> this.copy(journalpostId = journalpostId)
        is Unntak -> this.copy(journalpostId = journalpostId)
    }

    companion object {

        fun create(
            personident: Personident,
            veilederident: Veilederident,
            begrunnelse: String,
            document: List<DocumentComponent>,
            varselSvarfrist: LocalDate?,
            type: VurderingType,
        ): ManglendeMedvirkningVurdering =
            when (type) {
                VurderingType.FORHANDSVARSEL -> Forhandsvarsel(
                    uuid = UUID.randomUUID(),
                    personident = personident,
                    veilederident = veilederident,
                    createdAt = OffsetDateTime.now(),
                    begrunnelse = begrunnelse,
                    document = document,
                    journalpostId = null,
                    varsel = Varsel(
                        uuid = UUID.randomUUID(),
                        createdAt = OffsetDateTime.now(),
                        svarfrist = varselSvarfrist!!,
                    ),
                )
                VurderingType.IKKE_AKTUELL -> IkkeAktuell(
                    uuid = UUID.randomUUID(),
                    personident = personident,
                    veilederident = veilederident,
                    createdAt = OffsetDateTime.now(),
                    begrunnelse = begrunnelse,
                    document = document,
                    journalpostId = null,
                )
                VurderingType.STANS -> Stans(
                    uuid = UUID.randomUUID(),
                    personident = personident,
                    veilederident = veilederident,
                    createdAt = OffsetDateTime.now(),
                    begrunnelse = begrunnelse,
                    document = document,
                    journalpostId = null,
                )
                VurderingType.OPPFYLT -> Oppfylt(
                    uuid = UUID.randomUUID(),
                    personident = personident,
                    veilederident = veilederident,
                    createdAt = OffsetDateTime.now(),
                    begrunnelse = begrunnelse,
                    document = document,
                    journalpostId = null,
                )
                VurderingType.UNNTAK -> Unntak(
                    uuid = UUID.randomUUID(),
                    personident = personident,
                    veilederident = veilederident,
                    createdAt = OffsetDateTime.now(),
                    begrunnelse = begrunnelse,
                    document = document,
                    journalpostId = null,
                )
            }
    }
}

enum class VurderingType(val isActive: Boolean) {
    FORHANDSVARSEL(true),
    OPPFYLT(false),
    STANS(false),
    IKKE_AKTUELL(false),
    UNNTAK(false);

    fun brevkode(): BrevkodeType =
        when (this) {
            FORHANDSVARSEL -> BrevkodeType.MANGLENDE_MEDVIRKNING_FORHANDSVARSEL
            OPPFYLT, IKKE_AKTUELL, UNNTAK -> BrevkodeType.MANGLENDE_MEDVIRKNING_VURDERING
            STANS -> BrevkodeType.MANGLENDE_MEDVIRKNING_STANS
        }

    fun journalpostType(): JournalpostType =
        when (this) {
            FORHANDSVARSEL, OPPFYLT, IKKE_AKTUELL, UNNTAK -> JournalpostType.UTGAAENDE
            STANS -> JournalpostType.NOTAT
        }
}

data class Varsel(
    val uuid: UUID,
    val createdAt: OffsetDateTime,
    val svarfrist: LocalDate,
)
