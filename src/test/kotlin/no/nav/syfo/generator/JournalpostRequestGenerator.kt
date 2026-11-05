package no.nav.syfo.generator

import no.nav.syfo.UserConstants
import no.nav.syfo.common.journalforing.dto.AvsenderMottaker
import no.nav.syfo.common.journalforing.dto.Bruker
import no.nav.syfo.common.journalforing.dto.BrukerIdType
import no.nav.syfo.common.journalforing.dto.Dokument
import no.nav.syfo.common.journalforing.dto.Dokumentvariant
import no.nav.syfo.common.journalforing.dto.Filtype
import no.nav.syfo.common.journalforing.dto.JournalpostKanal
import no.nav.syfo.common.journalforing.dto.JournalpostRequest
import no.nav.syfo.common.journalforing.dto.JournalpostType
import no.nav.syfo.common.journalforing.dto.Variantformat
import no.nav.syfo.infrastructure.journalforing.BrevkodeType
import java.util.UUID

fun generateJournalpostRequest(
    tittel: String,
    brevkodeType: BrevkodeType,
    pdf: ByteArray,
    vurderingUuid: UUID,
    journalpostType: JournalpostType = JournalpostType.UTGAAENDE,
) = JournalpostRequest(
    avsenderMottaker = if (journalpostType == JournalpostType.NOTAT) {
        null
    } else {
        AvsenderMottaker(
            id = UserConstants.ARBEIDSTAKER_PERSONIDENT.value,
            idType = BrukerIdType.PERSONIDENT.value,
            navn = UserConstants.PERSON_FULLNAME,
        )
    },
    bruker = Bruker(
        id = UserConstants.ARBEIDSTAKER_PERSONIDENT.value,
        idType = BrukerIdType.PERSONIDENT.value
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
    journalpostType = journalpostType.value,
    eksternReferanseId = vurderingUuid.toString(),
    kanal = if (journalpostType == JournalpostType.NOTAT) null else JournalpostKanal.DITT_NAV.value,
)
