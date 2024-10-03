package no.nav.syfo.api.endpoints

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.syfo.api.model.*
import no.nav.syfo.application.VurderingService
import no.nav.syfo.application.model.NewVurderingRequestDTO
import no.nav.syfo.domain.Personident
import no.nav.syfo.infrastructure.NAV_PERSONIDENT_HEADER
import no.nav.syfo.infrastructure.clients.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.infrastructure.clients.veiledertilgang.validateVeilederAccess
import no.nav.syfo.util.getBearerHeader
import no.nav.syfo.util.getCallId
import no.nav.syfo.util.getNAVIdent
import no.nav.syfo.util.getPersonident

fun Route.registerManglendeMedvirkningEndpoints(
    veilederTilgangskontrollClient: VeilederTilgangskontrollClient,
    vurderingService: VurderingService,
) {
    route("/api/internad/v1/manglende-medvirkning") {
        get("/vurderinger") {
            val personident = call.getPersonident()
                ?: throw IllegalArgumentException("Failed to access manglende medvirkning for person: No $NAV_PERSONIDENT_HEADER supplied in request header")

            validateVeilederAccess(
                action = "GET /vurderinger",
                personident = personident,
                veilederTilgangskontrollClient = veilederTilgangskontrollClient,
            ) {
                val vurderinger = vurderingService.getVurderinger(
                    personident = personident,
                )
                val responseDTO = vurderinger.map { VurderingResponseDTO.fromVurdering(it) }
                if (responseDTO.isEmpty()) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.OK, responseDTO)
                }
            }
        }

        post("/vurderinger") {
            val requestDTO = call.receive<NewVurderingRequestDTO>()

            validateVeilederAccess(
                action = "POST /vurderinger",
                personident = Personident(requestDTO.personident),
                veilederTilgangskontrollClient = veilederTilgangskontrollClient,
            ) {
                val callId = call.getCallId()
                if (requestDTO.document.isEmpty()) {
                    throw IllegalArgumentException("Vurdering can't have empty document, callId: $callId")
                }

                val newVurdering = vurderingService.createNewVurdering(
                    veilederident = call.getNAVIdent(),
                    newVurdering = requestDTO,
                    callId = callId,
                )

                call.respond(HttpStatusCode.Created, VurderingResponseDTO.fromVurdering(newVurdering))
            }
        }

        post("/get-vurderinger") {
            val token = call.getBearerHeader()
                ?: throw IllegalArgumentException("Failed to get vurderinger for personer. No Authorization header supplied.")
            val requestDTOList = call.receive<VurderingerRequestDTO>()

            val personerVeilederHasAccessTo = veilederTilgangskontrollClient.veilederPersonerAccess(
                personidenter = requestDTOList.personidenter.map { Personident(it) },
                token = token,
                callId = call.getCallId(),
            )

            val vurderinger = if (personerVeilederHasAccessTo.isNullOrEmpty()) {
                emptyMap()
            } else {
                vurderingService.getLatestVurderingForPersoner(
                    personidenter = personerVeilederHasAccessTo,
                )
            }

            if (vurderinger.isEmpty()) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                val responseDTO = VurderingerResponseDTO(
                    vurderinger = vurderinger.map {
                        it.key.value to VurderingResponseDTO.fromVurdering(it.value)
                    }.associate { it },
                )
                call.respond(responseDTO)
            }
        }
    }
}
