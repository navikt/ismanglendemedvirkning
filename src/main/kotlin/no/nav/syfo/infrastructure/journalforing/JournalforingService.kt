package no.nav.syfo.infrastructure.journalforing

import no.nav.syfo.application.IJournalforingService
import no.nav.syfo.domain.*
import no.nav.syfo.infrastructure.clients.dokarkiv.DokarkivClient
import no.nav.syfo.infrastructure.clients.dokarkiv.dto.*
import no.nav.syfo.infrastructure.clients.pdl.PdlClient

class JournalforingService(
    private val dokarkivClient: DokarkivClient,
    private val pdlClient: PdlClient,
) : IJournalforingService {
    override suspend fun journalfor(
        personident: Personident,
        pdf: ByteArray,
        vurdering: ManglendeMedvirkningVurdering,
    ): Int {
        val navn = pdlClient.getPerson(personident).fullName
        val journalpostRequest = createJournalpostRequest(
            personIdent = personident,
            navn = navn,
            pdf = pdf,
            vurdering = vurdering,
        )

        return dokarkivClient.journalfor(journalpostRequest).journalpostId
    }

    private fun createJournalpostRequest(
        personIdent: Personident,
        navn: String,
        pdf: ByteArray,
        vurdering: ManglendeMedvirkningVurdering,
    ): JournalpostRequest {
        val avsenderMottaker = AvsenderMottaker(
            id = personIdent.value,
            idType = BrukerIdType.PERSON_IDENT.value,
            navn = navn,
        )
        val bruker = Bruker(
            id = personIdent.value,
            idType = BrukerIdType.PERSON_IDENT.value,
        )

        val dokumentTittel = vurdering.vurderingType.dokumentTittel()

        val dokumenter = listOf(
            Dokument(
                brevkode = vurdering.vurderingType.brevkode().value,
                dokumentvarianter = listOf(
                    Dokumentvariant(
                        filnavn = dokumentTittel,
                        filtype = Filtype.PDFA.value,
                        fysiskDokument = pdf,
                        variantformat = Variantformat.ARKIV.value,
                    )
                ),
                tittel = dokumentTittel,
            )
        )

        return JournalpostRequest(
            journalpostType = vurdering.vurderingType.journalpostType().name,
            avsenderMottaker = avsenderMottaker,
            tittel = dokumentTittel,
            bruker = bruker,
            dokumenter = dokumenter,
            eksternReferanseId = vurdering.uuid.toString(),
        )
    }
}
