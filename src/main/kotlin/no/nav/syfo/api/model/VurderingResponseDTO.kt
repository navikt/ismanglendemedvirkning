package no.nav.syfo.api.model

import no.nav.syfo.domain.*
import no.nav.syfo.util.toLocalDateTimeOslo
import java.time.LocalDate
import java.time.LocalDateTime
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
            createdAt = vurdering.createdAt.toLocalDateTimeOslo(),
            type = vurdering.vurderingType,
            begrunnelse = vurdering.begrunnelse,
            document = vurdering.document,
            varsel = if (vurdering is ManglendeMedvirkningVurdering.Forhandsvarsel) VarselDTO.fromVarsel(vurdering.varsel) else null,
        )
    }
}

data class VarselDTO private constructor(
    val uuid: UUID,
    val createdAt: LocalDateTime,
    val svarfrist: LocalDate,
) {
    companion object {
        fun fromVarsel(varsel: Varsel) = VarselDTO(
            uuid = varsel.uuid,
            createdAt = varsel.createdAt.toLocalDateTimeOslo(),
            svarfrist = varsel.svarfrist,
        )
    }
}
