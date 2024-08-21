package no.nav.syfo.api.model

import no.nav.syfo.domain.*
import java.time.LocalDateTime
import java.util.UUID

data class VurderingResponseDTO private constructor(
    val uuid: UUID,
    val personident: String,
    val createdAt: LocalDateTime,
    val type: VurderingType,
    val begrunnelse: String,
    val document: List<DocumentComponent>,
) {
    companion object {
        fun createFromVurdering(vurdering: ManglendeMedvirkningVurdering) = VurderingResponseDTO(
            uuid = vurdering.uuid,
            personident = vurdering.personident.value,
            createdAt = vurdering.createdAt.toLocalDateTime(),
            type = vurdering.vurderingType,
            begrunnelse = vurdering.begrunnelse,
            document = vurdering.document,
        )
    }
}
