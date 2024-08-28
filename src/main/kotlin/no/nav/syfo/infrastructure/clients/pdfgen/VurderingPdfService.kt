package no.nav.syfo.infrastructure.clients.pdfgen

import no.nav.syfo.application.IVurderingPdfService
import no.nav.syfo.domain.ManglendeMedvirkningVurdering
import no.nav.syfo.domain.VurderingType
import no.nav.syfo.infrastructure.clients.pdl.PdlClient

class VurderingPdfService(
    private val pdfGenClient: PdfGenClient,
    private val pdlClient: PdlClient,
) : IVurderingPdfService {

    override suspend fun createVurderingPdf(
        vurdering: ManglendeMedvirkningVurdering,
        callId: String,
    ): ByteArray {
        val personNavn = pdlClient.getPerson(vurdering.personident).fullName
        val vurderingPdfDTO = VurderingPdfDTO(
            documentComponents = vurdering.document,
            mottakerNavn = personNavn,
            mottakerPersonident = vurdering.personident,
        )

        return when (vurdering.vurderingType) {
            VurderingType.FORHANDSVARSEL -> pdfGenClient.createForhandsvarselPdf(
                callId = callId,
                forhandsvarselPdfDTO = vurderingPdfDTO,
            )
            VurderingType.OPPFYLT, VurderingType.IKKE_AKTUELL -> pdfGenClient.createVurderingPdf(
                callId = callId,
                vurderingPdfDTO = vurderingPdfDTO,
            )
            VurderingType.STANS -> pdfGenClient.createStansPdf(
                callId = callId,
                stansPdfDTO = vurderingPdfDTO,
            )
        }
    }
}
