package no.nav.syfo.infrastructure.kafka

import no.nav.syfo.application.IVarselProducer
import no.nav.syfo.domain.JournalpostId
import no.nav.syfo.domain.Personident
import no.nav.syfo.domain.Varsel
import no.nav.syfo.infrastructure.kafka.esyfovarsel.EsyfovarselHendelseProducer

class VarselProducer(
    private val arbeidstakerForhandsvarselProducer: EsyfovarselHendelseProducer,
) : IVarselProducer {

    override fun sendArbeidstakerForhandsvarsel(
        personIdent: Personident,
        journalpostId: JournalpostId,
        varsel: Varsel
    ): Result<Varsel> =
        arbeidstakerForhandsvarselProducer.sendArbeidstakerForhandsvarsel(
            personIdent = personIdent,
            journalpostId = journalpostId,
            varsel = varsel
        )
}
