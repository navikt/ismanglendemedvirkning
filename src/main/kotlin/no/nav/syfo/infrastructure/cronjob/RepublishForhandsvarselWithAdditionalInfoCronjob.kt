package no.nav.syfo.infrastructure.cronjob

import no.nav.syfo.application.VurderingService
import no.nav.syfo.application.model.NewVurderingRequestDTO
import no.nav.syfo.domain.DocumentComponent
import no.nav.syfo.domain.DocumentComponentType
import no.nav.syfo.domain.Vurdering
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class RepublishForhandsvarselWithAdditionalInfoCronjob(
    private val vurderingService: VurderingService,
    private val uuids: List<String> = emptyList(),
) : Cronjob {
    override val initialDelayMinutes: Long = 4
    override val intervalDelayMinutes: Long = 10000000

    private val log = LoggerFactory.getLogger(RepublishForhandsvarselWithAdditionalInfoCronjob::class.java)

    override suspend fun run(): List<Result<Vurdering>> {
        val newFrist = LocalDate.of(2025, 4, 9) // 9. april 2025
        val result = uuids.map { uuidString ->
            try {
                val uuid = UUID.fromString(uuidString)
                val vurdering = vurderingService.getVurdering(uuid)
                if (vurdering != null) {
                    val vurderingerForPerson = vurderingService.getVurderinger(vurdering.personident)
                    if (vurderingerForPerson.firstOrNull()?.uuid == uuid) {
                        val newDocument = generateNewDocument(vurdering, newFrist)
                        val newForhandsvarselDTO = NewVurderingRequestDTO.Forhandsvarsel(
                            personident = vurdering.personident.value,
                            begrunnelse = vurdering.begrunnelse,
                            document = newDocument,
                            varselSvarfrist = newFrist,
                        )
                        val newVurdering = vurderingService.createNewVurdering(
                            veilederident = vurdering.veilederident,
                            newVurdering = newForhandsvarselDTO,
                            callId = "cronjob-republish-forhandsvarsel",
                        )
                        Result.success(newVurdering)
                    } else {
                        Result.failure(IllegalArgumentException("Vurdering with UUID $uuid already revarslet"))
                    }
                } else {
                    Result.failure(IllegalArgumentException("Vurdering with UUID $uuid not found"))
                }
            } catch (e: Exception) {
                log.error("Exception caught while attempting to republish forhandsvarsel with UUID $uuidString", e)
                Result.failure(e)
            }
        }
        log.info(
            """
            Updated ${result.count { it.isSuccess }} forhandsvarsler in ${RepublishForhandsvarselWithAdditionalInfoCronjob::class.java.simpleName}.
            UUIDs for new forhandsvarsler: ${result.filter { it.isSuccess }.joinToString(", ") { it.getOrNull()?.uuid.toString() }}
            """.trimIndent()
        )

        return result
    }

    private fun generateNewDocument(vurdering: Vurdering, newFrist: LocalDate): List<DocumentComponent> {
        return vurdering.document.map { documentComponent ->
            if (documentComponent.type == DocumentComponentType.PARAGRAPH &&
                documentComponent.texts.any { it.contains("For å få sykepenger er det et vilkår at du medvirker i egen sak. Dette betyr at du blant annet har en plikt til å gi opplysninger til Nav, delta i dialogmøter og ta imot tilbud om tilrettelegging.") }
            ) {
                val extraText = "Viktig informasjon: På grunn av en teknisk feil, har vi ikke klart å varsle deg om dette brevet tidligere. Vi beklager ulempen. Dette brevet erstatter tidligere brev som du ikke ble varslet om, og det er kun dette brevet du skal forholde deg til. Det opprinnelige brevet kan du finne under Mine dokumenter på innloggede sider på nav.no.\n"
                DocumentComponent(
                    type = DocumentComponentType.PARAGRAPH,
                    title = null,
                    texts = listOf(
                        extraText,
                        "For å få sykepenger er det et vilkår at du medvirker i egen sak. Dette betyr at du blant annet har en plikt til å gi opplysninger til Nav, delta i dialogmøter og ta imot tilbud om tilrettelegging."
                    )
                )
            } else if (documentComponent.type == DocumentComponentType.PARAGRAPH &&
                documentComponent.texts.any { it.contains("Vi vurderer derfor å stanse sykepengene dine fra og med") }
            ) {
                DocumentComponent(
                    type = DocumentComponentType.PARAGRAPH,
                    title = null,
                    texts = listOf(
                        "Basert på opplysningene Nav har i saken har du ikke oppfylt plikten din til å medvirke, og det er heller ikke dokumentert at du hadde en rimelig grunn til å ikke medvirke. Vi vurderer derfor å stanse sykepengene dine fra og med ${newFrist.format(
                            DateTimeFormatter.ofPattern("dd.MM.yyyy")
                        )}."
                    )
                )
            } else if (documentComponent.type == DocumentComponentType.PARAGRAPH &&
                documentComponent.texts.any { it.contains("Vi ber om tilbakemelding fra deg innen") }
            ) {
                DocumentComponent(
                    type = DocumentComponentType.PARAGRAPH,
                    title = "Gi oss tilbakemelding",
                    texts = listOf(
                        "Vi ber om tilbakemelding fra deg innen ${newFrist.format(
                            DateTimeFormatter.ofPattern("dd.MM.yyyy")
                        )}. Etter denne datoen vil Nav vurdere å stanse sykepengene dine."
                    )
                )
            } else {
                documentComponent
            }
        }
    }
}
