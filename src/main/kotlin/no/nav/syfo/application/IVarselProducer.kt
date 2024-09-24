package no.nav.syfo.application

import no.nav.syfo.domain.JournalpostId
import no.nav.syfo.domain.Personident
import no.nav.syfo.domain.Varsel

interface IVarselProducer {
    fun sendArbeidstakerForhandsvarsel(
        personIdent: Personident,
        journalpostId: JournalpostId,
        varsel: Varsel
    ): Result<Varsel>
}
