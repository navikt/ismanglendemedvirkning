package no.nav.syfo.infrastructure.database.repository

import no.nav.syfo.domain.DocumentComponent
import no.nav.syfo.domain.JournalpostId
import no.nav.syfo.domain.Personident
import no.nav.syfo.domain.Veilederident
import no.nav.syfo.domain.Vurdering
import no.nav.syfo.domain.VurderingType
import java.time.OffsetDateTime
import java.util.UUID

data class PVurdering(
    val id: Int,
    val uuid: UUID,
    val personident: Personident,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
    val veilederident: Veilederident,
    val type: VurderingType,
    val begrunnelse: String,
    val document: List<DocumentComponent>,
    val journalpostId: JournalpostId?,
    val publishedAt: OffsetDateTime?,
) {

    fun toManglendeMedvirkningVurdering(pVarsel: PVarsel?) =
        when (type) {
            VurderingType.FORHANDSVARSEL -> Vurdering.Forhandsvarsel(
                uuid = uuid,
                personident = personident,
                veilederident = veilederident,
                createdAt = createdAt,
                begrunnelse = begrunnelse,
                document = document,
                journalpostId = journalpostId,
                varsel = pVarsel!!.toVarsel(),
            )
            VurderingType.OPPFYLT -> Vurdering.Oppfylt(
                uuid = uuid,
                personident = personident,
                veilederident = veilederident,
                createdAt = createdAt,
                begrunnelse = begrunnelse,
                document = document,
                journalpostId = journalpostId,
            )
            VurderingType.STANS -> Vurdering.Stans(
                uuid = uuid,
                personident = personident,
                veilederident = veilederident,
                createdAt = createdAt,
                begrunnelse = begrunnelse,
                document = document,
                journalpostId = journalpostId,
            )
            VurderingType.IKKE_AKTUELL -> Vurdering.IkkeAktuell(
                uuid = uuid,
                personident = personident,
                veilederident = veilederident,
                createdAt = createdAt,
                begrunnelse = begrunnelse,
                document = document,
                journalpostId = journalpostId,
            )
            VurderingType.UNNTAK -> Vurdering.Unntak(
                uuid = uuid,
                personident = personident,
                veilederident = veilederident,
                createdAt = createdAt,
                begrunnelse = begrunnelse,
                document = document,
                journalpostId = journalpostId,
            )
        }
}
