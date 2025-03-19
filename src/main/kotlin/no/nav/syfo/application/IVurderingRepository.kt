package no.nav.syfo.application

import no.nav.syfo.domain.Personident
import no.nav.syfo.domain.Vurdering
import java.util.UUID

interface IVurderingRepository {
    fun saveManglendeMedvirkningVurdering(
        vurdering: Vurdering,
        vurderingPdf: ByteArray,
    ): Vurdering

    fun setJournalpostId(vurdering: Vurdering)

    fun getNotJournalforteVurderinger(): List<Pair<Vurdering, ByteArray>>

    fun updatePublishedAt(vurderingUuid: UUID)

    fun getVurderinger(personident: Personident): List<Vurdering>

    fun getLatestVurderingForPersoner(personidenter: List<Personident>): Map<Personident, Vurdering>

    fun updatePersonident(nyPersonident: Personident, vurderinger: List<Vurdering>)

    fun getVurdering(uuid: UUID): Vurdering?
}
