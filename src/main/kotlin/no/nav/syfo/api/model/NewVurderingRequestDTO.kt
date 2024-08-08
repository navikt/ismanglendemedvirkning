package no.nav.syfo.api.model

import no.nav.syfo.domain.Personident
import no.nav.syfo.domain.VurderingType
import no.nav.syfo.domain.api.DocumentComponent
import java.time.LocalDate

data class NewVurderingRequestDTO(
    val personident: Personident,
    val vurderingType: VurderingType,
    val begrunnelse: String,
    val document: List<DocumentComponent>,
    val varselSvarfrist: LocalDate?,
)
