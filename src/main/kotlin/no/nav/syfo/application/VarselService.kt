package no.nav.syfo.application

import kotlinx.coroutines.CancellationException
import no.nav.syfo.domain.Varsel

class VarselService(
    private val varselRepository: IVarselRepository,
    private val dokdistClient: IDokdistClient,
) {

    suspend fun publishUnpublishedVarsler(): List<Result<Varsel>> {
        val unpublishedVarsler = varselRepository.getUnpublishedVarsler()

        return unpublishedVarsler.map { (_, journalpostId, varsel) ->
            runCatching {
                dokdistClient.distribuer(journalpostId)
                varselRepository.updatePublishedAt(varsel.uuid)
                varsel
            }.onFailure {
                if (it is CancellationException) throw it
            }
        }
    }
}
