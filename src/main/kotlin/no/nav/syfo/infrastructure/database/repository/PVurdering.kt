package no.nav.syfo.infrastructure.database.repository

import no.nav.syfo.domain.JournalpostId
import no.nav.syfo.domain.ManglendeMedvirkningVurdering
import no.nav.syfo.domain.Personident
import no.nav.syfo.domain.Varsel
import no.nav.syfo.domain.Veilederident
import no.nav.syfo.domain.VurderingType
import no.nav.syfo.domain.api.DocumentComponent
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
            VurderingType.FORHANDSVARSEL -> ManglendeMedvirkningVurdering.Forhandsvarsel(
                uuid = uuid,
                personident = personident,
                veilederident = veilederident,
                createdAt = createdAt,
                begrunnelse = begrunnelse,
                document = document,
                journalpostId = journalpostId,
                varsel = Varsel(
                    uuid = pVarsel!!.uuid,
                    createdAt = pVarsel.createdAt,
                    svarfrist = pVarsel.svarfrist,
                ),
            )
            VurderingType.OPPFYLT -> ManglendeMedvirkningVurdering.Oppfylt(
                uuid = uuid,
                personident = personident,
                veilederident = veilederident,
                createdAt = createdAt,
                begrunnelse = begrunnelse,
                document = document,
                journalpostId = journalpostId,
            )
            VurderingType.STANS -> ManglendeMedvirkningVurdering.Stans(
                uuid = uuid,
                personident = personident,
                veilederident = veilederident,
                createdAt = createdAt,
                begrunnelse = begrunnelse,
                document = document,
                journalpostId = journalpostId,
            )
            VurderingType.IKKE_AKTUELL -> ManglendeMedvirkningVurdering.IkkeAktuell(
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
