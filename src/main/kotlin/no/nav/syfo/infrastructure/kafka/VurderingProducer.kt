package no.nav.syfo.infrastructure.kafka

import no.nav.syfo.application.IVurderingProducer
import no.nav.syfo.domain.ManglendeMedvirkningVurdering
import no.nav.syfo.domain.Personident
import no.nav.syfo.domain.Varsel
import no.nav.syfo.domain.Veilederident
import no.nav.syfo.domain.VurderingType
import no.nav.syfo.util.configuredJacksonMapper
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serializer
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.util.UUID

class VurderingProducer(private val producer: KafkaProducer<String, VurderingRecord>) : IVurderingProducer {

    override fun publishVurdering(vurdering: ManglendeMedvirkningVurdering): Result<ManglendeMedvirkningVurdering> =
        try {
            producer.send(
                ProducerRecord(
                    TOPIC,
                    vurdering.personident.asProducerRecordKey(),
                    VurderingRecord.fromVurdering(vurdering),
                )
            )?.also { it.get() }
            Result.success(vurdering)
        } catch (e: Exception) {
            log.error("Exception was thrown when attempting to send vurdering: ${e.message}")
            Result.failure(e)
        }

    companion object {
        private const val TOPIC = "teamsykefravr.manglende-medvirkning-vurdering"
        private val log = LoggerFactory.getLogger(VurderingProducer::class.java)
    }
}

fun Personident.asProducerRecordKey(): String = UUID.nameUUIDFromBytes(value.toByteArray()).toString()

data class VurderingRecord(
    val uuid: UUID,
    val personident: Personident,
    val veilederident: Veilederident,
    val createdAt: OffsetDateTime,
    val begrunnelse: String,
    val varsel: Varsel?,
    val vurderingType: VurderingTypeDTO,
) {
    companion object {
        fun fromVurdering(vurdering: ManglendeMedvirkningVurdering): VurderingRecord =
            VurderingRecord(
                uuid = vurdering.uuid,
                personident = vurdering.personident,
                veilederident = vurdering.veilederident,
                createdAt = vurdering.createdAt,
                begrunnelse = vurdering.begrunnelse,
                varsel = when (vurdering) {
                    is ManglendeMedvirkningVurdering.Forhandsvarsel -> vurdering.varsel
                    else -> null
                },
                vurderingType = VurderingTypeDTO(
                    value = vurdering.vurderingType,
                    isActive = vurdering.vurderingType.isActive
                )
            )
    }
}

data class VurderingTypeDTO(
    val value: VurderingType,
    val isActive: Boolean,
)

class VurderingRecordSerializer : Serializer<VurderingRecord> {
    private val mapper = configuredJacksonMapper()
    override fun serialize(topic: String?, data: VurderingRecord?): ByteArray =
        mapper.writeValueAsBytes(data)
}
