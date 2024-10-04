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
    val vurderingType: VurderingType,
    val veilederident: String,
    val begrunnelse: String,
    val stansdato: LocalDate?,
    val document: List<DocumentComponent>,
    val varsel: VarselDTO?,
) {
    companion object {
        fun fromVurdering(vurdering: Vurdering) = VurderingResponseDTO(
            uuid = vurdering.uuid,
            personident = vurdering.personident.value,
            createdAt = vurdering.createdAt.toLocalDateTimeOslo(),
            vurderingType = vurdering.vurderingType,
            veilederident = vurdering.veilederident.value,
            begrunnelse = vurdering.begrunnelse,
            stansdato = if (vurdering is Vurdering.Stans) vurdering.stansdato else null,
            document = vurdering.document,
            varsel = if (vurdering is Vurdering.Forhandsvarsel) VarselDTO.fromVarsel(vurdering.varsel) else null,
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
