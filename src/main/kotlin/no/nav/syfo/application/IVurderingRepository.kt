package no.nav.syfo.application

import no.nav.syfo.domain.ManglendeMedvirkningVurdering
import java.util.UUID

interface IVurderingRepository {
    fun saveManglendeMedvirkningVurdering(
        vurdering: ManglendeMedvirkningVurdering,
        vurderingPdf: ByteArray,
    ): ManglendeMedvirkningVurdering

    fun setJournalpostId(vurdering: ManglendeMedvirkningVurdering)

    fun getNotJournalforteVurderinger(): List<Pair<ManglendeMedvirkningVurdering, ByteArray>>

    fun updatePublishedAt(vurderingUuid: UUID)
}
