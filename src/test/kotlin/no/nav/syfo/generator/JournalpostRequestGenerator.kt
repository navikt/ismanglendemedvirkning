package no.nav.syfo.generator

import no.nav.syfo.UserConstants
import no.nav.syfo.infrastructure.clients.dokarkiv.dto.*
import java.util.UUID

fun generateJournalpostRequest(
    tittel: String,
    brevkodeType: BrevkodeType,
    pdf: ByteArray,
    vurderingUuid: UUID,
    journalpostType: String = JournalpostType.UTGAAENDE.name,
) = JournalpostRequest(
    avsenderMottaker = AvsenderMottaker(
        id = UserConstants.ARBEIDSTAKER_PERSONIDENT.value,
        idType = BrukerIdType.PERSON_IDENT.value,
        navn = UserConstants.PERSON_FULLNAME,
    ),
    bruker = Bruker(
        id = UserConstants.ARBEIDSTAKER_PERSONIDENT.value,
        idType = BrukerIdType.PERSON_IDENT.value
    ),
    tittel = tittel,
    dokumenter = listOf(
        Dokument(
            brevkode = brevkodeType.value,
            tittel = tittel,
            dokumentvarianter = listOf(
                Dokumentvariant(
                    filnavn = tittel,
                    filtype = Filtype.PDFA.value,
                    fysiskDokument = pdf,
                    variantformat = Variantformat.ARKIV.value,
                )
            ),
        )
    ),
    journalpostType = journalpostType,
    eksternReferanseId = vurderingUuid.toString(),
)
