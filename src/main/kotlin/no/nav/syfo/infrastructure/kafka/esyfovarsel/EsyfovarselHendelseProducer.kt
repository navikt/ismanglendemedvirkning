package no.nav.syfo.infrastructure.kafka.esyfovarsel

import no.nav.syfo.domain.JournalpostId
import no.nav.syfo.domain.Personident
import no.nav.syfo.domain.Varsel
import no.nav.syfo.infrastructure.kafka.esyfovarsel.dto.ArbeidstakerHendelse
import no.nav.syfo.infrastructure.kafka.esyfovarsel.dto.EsyfovarselHendelse
import no.nav.syfo.infrastructure.kafka.esyfovarsel.dto.HendelseType
import no.nav.syfo.infrastructure.kafka.esyfovarsel.dto.VarselData
import no.nav.syfo.infrastructure.kafka.esyfovarsel.dto.VarselDataJournalpost
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import java.util.*

class EsyfovarselHendelseProducer(
    private val producer: KafkaProducer<String, EsyfovarselHendelse>,
) {
    fun sendArbeidstakerForhandsvarsel(
        personIdent: Personident,
        journalpostId: JournalpostId,
        varsel: Varsel
    ): Result<Varsel> {
        val varselHendelse = ArbeidstakerHendelse(
            type = HendelseType.SM_FORHANDSVARSEL_MANGLENDE_MEDVIRKNING,
            arbeidstakerFnr = personIdent.value,
            data = VarselData(
                journalpost = VarselDataJournalpost(
                    uuid = varsel.uuid.toString(),
                    id = journalpostId.value,
                ),
            ),
            orgnummer = null,
        )

        return try {
            producer.send(
                ProducerRecord(
                    ESYFOVARSEL_TOPIC,
                    UUID.randomUUID().toString(),
                    varselHendelse,
                )
            ).get()
            Result.success(varsel)
        } catch (e: Exception) {
            log.error("Exception was thrown when attempting to send hendelse varsel (uuid: ${varsel.uuid}) to esyfovarsel: ${e.message}")
            Result.failure(e)
        }
    }

    companion object {
        private const val ESYFOVARSEL_TOPIC = "team-esyfo.varselbus"
        private val log = LoggerFactory.getLogger(EsyfovarselHendelseProducer::class.java)
    }
}
