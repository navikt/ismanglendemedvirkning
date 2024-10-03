package no.nav.syfo.generator

import no.nav.syfo.UserConstants
import no.nav.syfo.domain.*
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

fun generateVurdering(
    personident: Personident = UserConstants.ARBEIDSTAKER_PERSONIDENT,
    begrunnelse: String = "En begrunnelse",
    document: List<DocumentComponent> = generateDocumentComponent(begrunnelse),
    type: VurderingType,
    createdAt: OffsetDateTime = OffsetDateTime.now(),
) = when (type) {
    VurderingType.FORHANDSVARSEL -> Vurdering.Forhandsvarsel(
        uuid = UUID.randomUUID(),
        createdAt = createdAt,
        personident = personident,
        veilederident = UserConstants.VEILEDER_IDENT,
        begrunnelse = begrunnelse,
        document = document,
        journalpostId = null,
        varsel = Varsel(
            uuid = UUID.randomUUID(),
            createdAt = OffsetDateTime.now(),
            svarfrist = LocalDate.now().plusWeeks(3),
        ),
    )
    VurderingType.OPPFYLT -> Vurdering.Oppfylt(
        uuid = UUID.randomUUID(),
        createdAt = createdAt,
        personident = personident,
        veilederident = UserConstants.VEILEDER_IDENT,
        begrunnelse = begrunnelse,
        document = document,
        journalpostId = null,
    )
    VurderingType.IKKE_AKTUELL -> Vurdering.IkkeAktuell(
        uuid = UUID.randomUUID(),
        createdAt = createdAt,
        personident = personident,
        veilederident = UserConstants.VEILEDER_IDENT,
        begrunnelse = begrunnelse,
        document = document,
        journalpostId = null,
    )
    VurderingType.STANS -> Vurdering.Stans(
        uuid = UUID.randomUUID(),
        createdAt = createdAt,
        personident = personident,
        veilederident = UserConstants.VEILEDER_IDENT,
        begrunnelse = begrunnelse,
        document = document,
        journalpostId = null,
    )
    VurderingType.UNNTAK -> Vurdering.Unntak(
        uuid = UUID.randomUUID(),
        createdAt = createdAt,
        personident = personident,
        veilederident = UserConstants.VEILEDER_IDENT,
        begrunnelse = begrunnelse,
        document = document,
        journalpostId = null,
    )
}
