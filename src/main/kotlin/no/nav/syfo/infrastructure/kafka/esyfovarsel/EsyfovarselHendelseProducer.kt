package no.nav.syfo.infrastructure.kafka.esyfovarsel

import no.nav.syfo.domain.ManglendeMedvirkningVurdering
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

    fun sendVurderingVarsel(vurdering: ManglendeMedvirkningVurdering): Result<ManglendeMedvirkningVurdering> {
        if (vurdering.journalpostId == null)
            throw IllegalStateException("JournalpostId is null for vurdering ${vurdering.uuid}")

        val varselHendelse = ArbeidstakerHendelse(
            type = HendelseType.SM_FORHANDSVARSEL_MANGLENDE_MEDVIRKNING,
            arbeidstakerFnr = vurdering.personident.value,
            data = VarselData(
                journalpost = VarselDataJournalpost(
                    uuid = vurdering.uuid.toString(),
                    id = vurdering.journalpostId!!.value,
                ),
            ),
            orgnummer = null,
        )

        return try {
            producer.send(
                ProducerRecord(
                    ESYFOVARSEL_TOPIC,
                    UUID.nameUUIDFromBytes(vurdering.personident.value.toByteArray()).toString(),
                    varselHendelse,
                )
            ).get()
            Result.success(vurdering)
        } catch (e: Exception) {
            log.error("Exception was thrown when attempting to send hendelse varsel (uuid: ${vurdering.uuid}) to esyfovarsel: ${e.message}")
            Result.failure(e)
        }
    }

    companion object {
        private const val ESYFOVARSEL_TOPIC = "team-esyfo.varselbus"
        private val log = LoggerFactory.getLogger(EsyfovarselHendelseProducer::class.java)
    }
}
