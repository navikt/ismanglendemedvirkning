package no.nav.syfo.application

import no.nav.syfo.domain.Varsel

class VarselService(
    private val varselRepository: IVarselRepository,
    private val varselProducer: IVarselProducer
) {

    fun publishUnpublishedVarsler(): List<Result<Varsel>> {
        val unpublishedVarsler = varselRepository.getUnpublishedVarsler()

        return unpublishedVarsler.map { (personident, journalpostId, varsel) ->
            varselProducer.sendArbeidstakerForhandsvarsel(
                personIdent = personident,
                journalpostId = journalpostId,
                varsel = varsel,
            ).map {
                varselRepository.updatePublishedAt(it.uuid)
                it
            }
        }
    }
}
