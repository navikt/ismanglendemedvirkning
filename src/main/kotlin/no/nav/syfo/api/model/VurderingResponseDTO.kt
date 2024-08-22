package no.nav.syfo.api.model

import no.nav.syfo.domain.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.UUID

data class VurderingResponseDTO private constructor(
    val uuid: UUID,
    val personident: String,
    val createdAt: LocalDateTime,
    val type: VurderingType,
    val begrunnelse: String,
    val document: List<DocumentComponent>,
    val varsel: VarselDTO?,
) {
    companion object {
        fun fromVurdering(vurdering: ManglendeMedvirkningVurdering) = VurderingResponseDTO(
            uuid = vurdering.uuid,
            personident = vurdering.personident.value,
            createdAt = vurdering.createdAt.toLocalDateTime(),
            type = vurdering.vurderingType,
            begrunnelse = vurdering.begrunnelse,
            document = vurdering.document,
            varsel = if (vurdering is ManglendeMedvirkningVurdering.Forhandsvarsel) VarselDTO.fromVarsel(vurdering.varsel) else null,
        )
    }
}

data class VarselDTO private constructor(
    val uuid: UUID,
    val createdAt: OffsetDateTime,
    val svarfrist: LocalDate,
) {
    companion object {
        fun fromVarsel(varsel: Varsel) = VarselDTO(
            uuid = varsel.uuid,
            createdAt = varsel.createdAt,
            svarfrist = varsel.svarfrist,
        )
    }
}
