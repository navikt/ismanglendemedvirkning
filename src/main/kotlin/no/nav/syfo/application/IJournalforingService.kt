package no.nav.syfo.application

import no.nav.syfo.domain.Personident
import no.nav.syfo.domain.Vurdering

interface IJournalforingService {
    suspend fun journalfor(
        personident: Personident,
        pdf: ByteArray,
        vurdering: Vurdering,
    ): Int
}
