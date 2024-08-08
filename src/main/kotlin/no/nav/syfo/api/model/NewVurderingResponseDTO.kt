package no.nav.syfo.api.model

import no.nav.syfo.domain.JournalpostId
import no.nav.syfo.domain.ManglendeMedvirkningVurdering
import no.nav.syfo.domain.Personident
import no.nav.syfo.domain.Varsel
import no.nav.syfo.domain.Veilederident
import no.nav.syfo.domain.VurderingType
import no.nav.syfo.domain.api.DocumentComponent
import java.time.OffsetDateTime
import java.util.UUID

data class NewVurderingResponseDTO(
    val uuid: UUID,
    val personident: Personident,
    val veilederident: Veilederident,
    val createdAt: OffsetDateTime,
    val begrunnelse: String,
    val document: List<DocumentComponent>,
    val journalpostId: JournalpostId?,
    val vurderingType: VurderingType,
    val varsel: Varsel?,
) {

    companion object {

        fun fromVurdering(vurdering: ManglendeMedvirkningVurdering): NewVurderingResponseDTO =
            NewVurderingResponseDTO(
                uuid = vurdering.uuid,
                personident = vurdering.personident,
                veilederident = vurdering.veilederident,
                createdAt = vurdering.createdAt,
                begrunnelse = vurdering.begrunnelse,
                document = vurdering.document,
                journalpostId = vurdering.journalpostId,
                vurderingType = vurdering.vurderingType,
                varsel = when (vurdering) {
                    is ManglendeMedvirkningVurdering.Forhandsvarsel -> vurdering.varsel
                    else -> null
                },
            )
    }
}
