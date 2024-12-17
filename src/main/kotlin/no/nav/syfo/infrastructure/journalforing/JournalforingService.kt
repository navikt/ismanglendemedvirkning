package no.nav.syfo.infrastructure.journalforing

import no.nav.syfo.application.IJournalforingService
import no.nav.syfo.domain.*
import no.nav.syfo.infrastructure.clients.dokarkiv.DokarkivClient
import no.nav.syfo.infrastructure.clients.dokarkiv.dto.*
import no.nav.syfo.infrastructure.clients.pdl.PdlClient
import org.slf4j.LoggerFactory

class JournalforingService(
    private val dokarkivClient: DokarkivClient,
    private val pdlClient: PdlClient,
    private val isJournalforingRetryEnabeled: Boolean,
) : IJournalforingService {
    override suspend fun journalfor(
        personident: Personident,
        pdf: ByteArray,
        vurdering: Vurdering,
    ): Int {
        val navn = pdlClient.getPerson(personident).fullName
        val journalpostRequest = createJournalpostRequest(
            personIdent = personident,
            navn = navn,
            pdf = pdf,
            vurdering = vurdering,
        )

        return try {
            dokarkivClient.journalfor(journalpostRequest).journalpostId
        } catch (exc: Exception) {
            if (isJournalforingRetryEnabeled) {
                throw exc
            } else {
                log.error("Journalføring failed, skipping retry (should only happen in dev-gcp)", exc)
                // Defaulting'en til DEFAULT_FAILED_JP_ID skal bare forekomme i dev-gcp:
                // Har dette fordi vi ellers spammer ned dokarkiv med forsøk på å journalføre
                // på personer som mangler aktør-id.
                DEFAULT_FAILED_JP_ID
            }
        }
    }

    private fun createJournalpostRequest(
        personIdent: Personident,
        navn: String,
        pdf: ByteArray,
        vurdering: Vurdering,
    ): JournalpostRequest {
        val journalpostType = JournalpostType.fromVurderingType(vurdering.vurderingType)
        val avsenderMottaker = if (journalpostType == JournalpostType.NOTAT) {
            null
        } else {
            AvsenderMottaker(
                id = personIdent.value,
                idType = BrukerIdType.PERSON_IDENT.value,
                navn = navn,
            )
        }
        val bruker = Bruker(
            id = personIdent.value,
            idType = BrukerIdType.PERSON_IDENT.value,
        )

        val dokumentTittel = vurdering.vurderingType.dokumentTittel()

        val dokumenter = listOf(
            Dokument(
                brevkode = BrevkodeType.fromVurderingType(vurdering.vurderingType).value,
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
            journalpostType = JournalpostType.fromVurderingType(vurdering.vurderingType).name,
            avsenderMottaker = avsenderMottaker,
            tittel = dokumentTittel,
            bruker = bruker,
            dokumenter = dokumenter,
            eksternReferanseId = vurdering.uuid.toString(),
        )
    }

    companion object {
        const val DEFAULT_FAILED_JP_ID = 0
        private val log = LoggerFactory.getLogger(JournalforingService::class.java)
    }
}

fun VurderingType.dokumentTittel(): String =
    when (this) {
        VurderingType.FORHANDSVARSEL -> "Forhåndsvarsel om stans av sykepenger"
        VurderingType.OPPFYLT, VurderingType.IKKE_AKTUELL, VurderingType.UNNTAK -> "Vurdering av § 8-8 manglende medvirkning"
        VurderingType.STANS -> "Innstilling om stans"
    }
