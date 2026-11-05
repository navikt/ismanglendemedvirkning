package no.nav.syfo.infrastructure.journalforing

import no.nav.syfo.application.IJournalforingService
import no.nav.syfo.common.journalforing.JournalpostMottaker
import no.nav.syfo.common.journalforing.client.DokarkivClient
import no.nav.syfo.common.journalforing.createJournalpostRequest
import no.nav.syfo.common.journalforing.dto.Bruker
import no.nav.syfo.common.journalforing.dto.BrukerIdType
import no.nav.syfo.common.journalforing.dto.JournalpostKanal
import no.nav.syfo.common.journalforing.dto.JournalpostRequest
import no.nav.syfo.common.journalforing.dto.JournalpostType
import no.nav.syfo.domain.Personident
import no.nav.syfo.domain.Vurdering
import no.nav.syfo.domain.VurderingType
import no.nav.syfo.infrastructure.clients.pdl.PdlClient
import no.nav.syfo.common.types.ident.Personident as CommonPersonident
import org.slf4j.LoggerFactory

class JournalforingService(
    private val dokarkivClient: DokarkivClient,
    private val pdlClient: PdlClient,
    private val isJournalforingRetryEnabled: Boolean,
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
            dokarkivClient.journalfor(journalpostRequest).journalpostId.value.toInt()
        } catch (exc: Exception) {
            if (isJournalforingRetryEnabled) {
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
        val journalpostType = vurdering.vurderingType.journalpostType()
        val isNotat = journalpostType == JournalpostType.NOTAT

        return createJournalpostRequest(
            bruker = Bruker(
                id = personIdent.value,
                idType = BrukerIdType.PERSONIDENT.value,
            ),
            brevkode = BrevkodeType.fromVurderingType(vurdering.vurderingType),
            tittel = vurdering.vurderingType.dokumentTittel(),
            pdf = pdf,
            eksternReferanseId = vurdering.uuid.toString(),
            journalpostType = journalpostType,
            mottaker = if (isNotat) null else JournalpostMottaker.Person(CommonPersonident(personIdent.value), navn),
            kanal = if (isNotat) null else JournalpostKanal.DITT_NAV,
        )
    }

    companion object {
        const val DEFAULT_FAILED_JP_ID = 0
        private val log = LoggerFactory.getLogger(JournalforingService::class.java)
    }
}

private fun VurderingType.journalpostType(): JournalpostType =
    when (this) {
        VurderingType.FORHANDSVARSEL, VurderingType.OPPFYLT, VurderingType.IKKE_AKTUELL, VurderingType.UNNTAK -> JournalpostType.UTGAAENDE
        VurderingType.STANS -> JournalpostType.NOTAT
    }

fun VurderingType.dokumentTittel(): String =
    when (this) {
        VurderingType.FORHANDSVARSEL -> "Forhåndsvarsel om stans av sykepenger"
        VurderingType.OPPFYLT, VurderingType.IKKE_AKTUELL, VurderingType.UNNTAK -> "Vurdering av § 8-8 manglende medvirkning"
        VurderingType.STANS -> "Innstilling om stans"
    }
