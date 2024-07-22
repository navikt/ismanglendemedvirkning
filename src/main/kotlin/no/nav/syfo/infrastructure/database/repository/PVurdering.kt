package no.nav.syfo.infrastructure.database.repository

import no.nav.syfo.aktivitetskrav.api.DocumentComponentDTO
import no.nav.syfo.domain.JournalpostId
import no.nav.syfo.domain.ManglendeMedvirkningVurdering
import no.nav.syfo.domain.Personident
import no.nav.syfo.domain.Status
import no.nav.syfo.domain.Varsel
import java.time.OffsetDateTime
import java.util.UUID

data class PVurdering(
    val id: Int,
    val uuid: UUID,
    val personident: Personident,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
    val veilederident: String,
    val type: Status,
    val begrunnelse: String,
    val document: List<DocumentComponentDTO>,
    val journalpostId: JournalpostId?,
    val publishedAt: OffsetDateTime?,
) {

    fun toManglendeMedvirkningVurdering(pVarsel: PVarsel?) =
        when (type) {
            Status.FORHANDSVARSEL -> ManglendeMedvirkningVurdering.Forhandsvarsel(
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
            Status.OPPFYLT -> ManglendeMedvirkningVurdering.Oppfylt(
                uuid = uuid,
                personident = personident,
                veilederident = veilederident,
                createdAt = createdAt,
                begrunnelse = begrunnelse,
                document = document,
                journalpostId = journalpostId,
            )
            Status.STANS -> ManglendeMedvirkningVurdering.Stans(
                uuid = uuid,
                personident = personident,
                veilederident = veilederident,
                createdAt = createdAt,
                begrunnelse = begrunnelse,
                document = document,
                journalpostId = journalpostId,
            )
            Status.IKKE_AKTUELL -> ManglendeMedvirkningVurdering.IkkeAktuell(
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
