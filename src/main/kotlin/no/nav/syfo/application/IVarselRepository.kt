package no.nav.syfo.application

import no.nav.syfo.domain.JournalpostId
import no.nav.syfo.domain.Personident
import no.nav.syfo.domain.Varsel
import java.util.*

interface IVarselRepository {
    fun getUnpublishedVarsler(): List<Triple<Personident, JournalpostId, Varsel>>
    fun updatePublishedAt(varselUuid: UUID)
}
