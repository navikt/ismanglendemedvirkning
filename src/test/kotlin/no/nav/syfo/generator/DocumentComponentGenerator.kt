package no.nav.syfo.generator

import no.nav.syfo.domain.DocumentComponent
import no.nav.syfo.domain.DocumentComponentType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun generateDocumentComponent(fritekst: String, header: String = "Standard header") = listOf(
    DocumentComponent(
        type = DocumentComponentType.HEADER_H1,
        title = null,
        texts = listOf(header),
    ),
    DocumentComponent(
        type = DocumentComponentType.PARAGRAPH,
        title = null,
        texts = listOf(fritekst),
    ),
    DocumentComponent(
        type = DocumentComponentType.PARAGRAPH,
        key = "Standardtekst",
        title = null,
        texts = listOf("Dette er en standardtekst"),
    ),
)

fun generateForhandsvarselRevarslingDocumentComponent(beskrivelse: String, svarfrist: LocalDate) = listOf(
    DocumentComponent(
        type = DocumentComponentType.HEADER_H1,
        title = null,
        texts = listOf("Varsel om mulig stans av sykepenger"),
    ),
    DocumentComponent(
        type = DocumentComponentType.PARAGRAPH,
        title = null,
        texts = listOf("For å få sykepenger er det et vilkår at du medvirker i egen sak. Dette betyr at du blant annet har en plikt til å gi opplysninger til Nav, delta i dialogmøter og ta imot tilbud om tilrettelegging."),
    ),
    DocumentComponent(
        type = DocumentComponentType.PARAGRAPH,
        title = null,
        texts = listOf(
            "Basert på opplysningene Nav har i saken har du ikke oppfylt plikten din til å medvirke, og det er heller ikke dokumentert at du hadde en rimelig grunn til å ikke medvirke. Vi vurderer derfor å stanse sykepengene dine fra og med ${svarfrist.format(
                DateTimeFormatter.ofPattern("dd.MM.yyyy")
            )}."
        ),
    ),
    DocumentComponent(
        type = DocumentComponentType.PARAGRAPH,
        title = null,
        texts = listOf("Vi har ikke tatt en endelig avgjørelse om å stanse dine sykepenger."),
    ),
    DocumentComponent(
        type = DocumentComponentType.PARAGRAPH,
        title = null,
        texts = listOf(beskrivelse),
    ),
    DocumentComponent(
        type = DocumentComponentType.PARAGRAPH,
        title = "Gi oss tilbakemelding",
        texts = listOf(
            "Vi ber om tilbakemelding fra deg innen ${svarfrist.format(
                DateTimeFormatter.ofPattern("dd.MM.yyyy")
            )}. Etter denne datoen vil Nav vurdere å stanse sykepengene dine."
        ),
    ),
    DocumentComponent(
        type = DocumentComponentType.PARAGRAPH,
        title = "Kontaktinformasjon",
        texts = listOf("Kontakt oss gjerne på nav.no/skriv-til-oss eller telefon 55 55 33 33."),
    ),
    DocumentComponent(
        type = DocumentComponentType.PARAGRAPH,
        title = "Lovhjemmel",
        texts = listOf("Krav til medvirkning i egen sak er beskrevet i folketrygdloven § 8-8 første og tredje ledd."),
    ),
    DocumentComponent(
        type = DocumentComponentType.PARAGRAPH,
        title = null,
        texts = listOf("«Medlemmet har plikt til å gi opplysninger til arbeidsgiveren og Arbeids- og velferdsetaten om egen funksjonsevne og bidra til at hensiktsmessige tiltak for å tilrettelegge arbeidet og utprøving av funksjonsevnen blir utredet og iverksatt, se også § 21-3. Medlemmet plikter også å medvirke ved utarbeiding og gjennomføring av oppfølgingsplaner og delta i dialogmøter som nevnt i arbeidsmiljøloven § 4-6 og folketrygdloven § 8-7 a. (…)"),
    ),
    DocumentComponent(
        type = DocumentComponentType.PARAGRAPH,
        title = null,
        texts = listOf("Retten til sykepenger faller bort dersom medlemmet uten rimelig grunn nekter å gi opplysninger eller medvirke til utredning, eller uten rimelig grunn nekter å ta imot tilbud om behandling, rehabilitering, tilrettelegging av arbeid og arbeidsutprøving eller arbeidsrettede tiltak, se også § 21-8. Det samme gjelder dersom medlemmet uten rimelig grunn unnlater å medvirke ved utarbeiding og gjennomføring av oppfølgingsplaner, unnlater å delta i dialogmøter som nevnt i første ledd, eller unnlater å være i arbeidsrelatert aktivitet som nevnt i andre ledd.»"),
    ),
    DocumentComponent(
        type = DocumentComponentType.PARAGRAPH,
        title = null,
        texts = listOf("Med vennlig hilsen", "VEILEDER_NAVN", "Nav"),
    ),
)
