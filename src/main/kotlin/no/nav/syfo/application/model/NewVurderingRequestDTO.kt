package no.nav.syfo.application.model

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.syfo.domain.DocumentComponent
import java.time.LocalDate

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "vurderingType")
sealed class NewVurderingRequestDTO {
    abstract val personident: String
    abstract val begrunnelse: String
    abstract val document: List<DocumentComponent>

    @JsonTypeName("FORHANDSVARSEL")
    data class Forhandsvarsel(
        override val personident: String,
        override val begrunnelse: String,
        override val document: List<DocumentComponent>,
        val varselSvarfrist: LocalDate,
    ) : NewVurderingRequestDTO()

    @JsonTypeName("OPPFYLT")
    data class Oppfylt(
        override val personident: String,
        override val begrunnelse: String,
        override val document: List<DocumentComponent>,
    ) : NewVurderingRequestDTO()

    @JsonTypeName("STANS")
    data class Stans(
        override val personident: String,
        override val begrunnelse: String,
        override val document: List<DocumentComponent>,
        val stansdato: LocalDate,
    ) : NewVurderingRequestDTO()

    @JsonTypeName("IKKE_AKTUELL")
    data class IkkeAktuell(
        override val personident: String,
        override val begrunnelse: String,
        override val document: List<DocumentComponent>,
    ) : NewVurderingRequestDTO()

    @JsonTypeName("UNNTAK")
    data class Unntak(
        override val personident: String,
        override val begrunnelse: String,
        override val document: List<DocumentComponent>,
    ) : NewVurderingRequestDTO()
}
