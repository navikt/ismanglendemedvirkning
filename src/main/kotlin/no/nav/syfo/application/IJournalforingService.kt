package no.nav.syfo.application

import no.nav.syfo.domain.ManglendeMedvirkningVurdering
import no.nav.syfo.domain.Personident

interface IJournalforingService {
    suspend fun journalfor(
        personident: Personident,
        pdf: ByteArray,
        vurdering: ManglendeMedvirkningVurdering,
    ): Int
}
