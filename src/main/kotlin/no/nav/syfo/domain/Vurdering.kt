package no.nav.syfo.domain

import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

interface IVurdering {
    val uuid: UUID
    val personident: Personident
    val veilederident: Veilederident
    val createdAt: OffsetDateTime
    val begrunnelse: String
    val document: List<DocumentComponent>
    val journalpostId: JournalpostId?
}

sealed class Vurdering(val vurderingType: VurderingType) : IVurdering {

    data class Forhandsvarsel(
        override val uuid: UUID,
        override val personident: Personident,
        override val veilederident: Veilederident,
        override val createdAt: OffsetDateTime,
        override val begrunnelse: String,
        override val document: List<DocumentComponent>,
        override val journalpostId: JournalpostId?,
        val varsel: Varsel,
    ) : Vurdering(VurderingType.FORHANDSVARSEL) {
        companion object {
            private val FORHANDSVARSEL_ALLOWED_SVARFRIST_DAYS_SHORTEST = 21L
            private val FORHANDSVARSEL_ALLOWED_SVARFRIST_DAYS_LONGEST = 42L

            fun hasValidSvarfrist(svarfrist: LocalDate): Boolean {
                val allowedSvarfristShortest = LocalDate.now().plusDays(FORHANDSVARSEL_ALLOWED_SVARFRIST_DAYS_SHORTEST)
                val allowedSvarfristLongest = LocalDate.now().plusDays(FORHANDSVARSEL_ALLOWED_SVARFRIST_DAYS_LONGEST)
                return !(svarfrist.isBefore(allowedSvarfristShortest) || svarfrist.isAfter(allowedSvarfristLongest))
            }
        }
    }

    data class Oppfylt(
        override val uuid: UUID,
        override val personident: Personident,
        override val veilederident: Veilederident,
        override val createdAt: OffsetDateTime,
        override val begrunnelse: String,
        override val document: List<DocumentComponent>,
        override val journalpostId: JournalpostId?,
    ) : Vurdering(VurderingType.OPPFYLT)

    data class Stans(
        override val uuid: UUID,
        override val personident: Personident,
        override val veilederident: Veilederident,
        override val createdAt: OffsetDateTime,
        override val begrunnelse: String,
        override val document: List<DocumentComponent>,
        override val journalpostId: JournalpostId?,
        val stansdato: LocalDate,
    ) : Vurdering(VurderingType.STANS)

    data class IkkeAktuell(
        override val uuid: UUID,
        override val personident: Personident,
        override val veilederident: Veilederident,
        override val createdAt: OffsetDateTime,
        override val begrunnelse: String,
        override val document: List<DocumentComponent>,
        override val journalpostId: JournalpostId?,
    ) : Vurdering(VurderingType.IKKE_AKTUELL)

    data class Unntak(
        override val uuid: UUID,
        override val personident: Personident,
        override val veilederident: Veilederident,
        override val createdAt: OffsetDateTime,
        override val begrunnelse: String,
        override val document: List<DocumentComponent>,
        override val journalpostId: JournalpostId?,
    ) : Vurdering(VurderingType.UNNTAK)

    fun journalfor(journalpostId: JournalpostId): Vurdering = when (this) {
        is Forhandsvarsel -> this.copy(journalpostId = journalpostId)
        is Oppfylt -> this.copy(journalpostId = journalpostId)
        is Stans -> this.copy(journalpostId = journalpostId)
        is IkkeAktuell -> this.copy(journalpostId = journalpostId)
        is Unntak -> this.copy(journalpostId = journalpostId)
    }

    companion object {
        fun createForhandsvarsel(
            personident: Personident,
            veilederident: Veilederident,
            begrunnelse: String,
            document: List<DocumentComponent>,
            varselSvarfrist: LocalDate?,
        ): Forhandsvarsel =
            Forhandsvarsel(
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

        fun createIkkeAktuell(
            personident: Personident,
            veilederident: Veilederident,
            begrunnelse: String,
            document: List<DocumentComponent>,
        ) = IkkeAktuell(
            uuid = UUID.randomUUID(),
            personident = personident,
            veilederident = veilederident,
            createdAt = OffsetDateTime.now(),
            begrunnelse = begrunnelse,
            document = document,
            journalpostId = null,
        )

        fun createOppfylt(
            personident: Personident,
            veilederident: Veilederident,
            begrunnelse: String,
            document: List<DocumentComponent>,
        ) = Oppfylt(
            uuid = UUID.randomUUID(),
            personident = personident,
            veilederident = veilederident,
            createdAt = OffsetDateTime.now(),
            begrunnelse = begrunnelse,
            document = document,
            journalpostId = null,
        )

        fun createStans(
            personident: Personident,
            veilederident: Veilederident,
            begrunnelse: String,
            stansdato: LocalDate,
            document: List<DocumentComponent>,
        ) = Stans(
            uuid = UUID.randomUUID(),
            personident = personident,
            veilederident = veilederident,
            createdAt = OffsetDateTime.now(),
            begrunnelse = begrunnelse,
            stansdato = stansdato,
            document = document,
            journalpostId = null,
        )

        fun createUnntak(
            personident: Personident,
            veilederident: Veilederident,
            begrunnelse: String,
            document: List<DocumentComponent>,
        ) = Unntak(
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

enum class VurderingType(val isActive: Boolean) {
    FORHANDSVARSEL(true),
    OPPFYLT(false),
    STANS(false),
    IKKE_AKTUELL(false),
    UNNTAK(false);
}

data class Varsel(
    val uuid: UUID,
    val createdAt: OffsetDateTime,
    val svarfrist: LocalDate,
)
