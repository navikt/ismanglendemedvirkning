package no.nav.syfo.api.endpoints

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.syfo.api.model.NewVurderingRequestDTO
import no.nav.syfo.api.model.NewVurderingResponseDTO
import no.nav.syfo.application.VurderingService
import no.nav.syfo.infrastructure.clients.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.infrastructure.clients.veiledertilgang.validateVeilederAccess
import no.nav.syfo.util.getCallId
import no.nav.syfo.util.getNAVIdent

fun Route.registerManglendeMedvirkningEndpoints(
    veilederTilgangskontrollClient: VeilederTilgangskontrollClient,
    vurderingService: VurderingService,
) {
    route("/api/internad/v1/manglende-medvirkning") {
        get("/vurderinger") {
            // TODO: Implement
            call.respond(HttpStatusCode.NotImplemented)
        }

        post("/vurderinger") {
            val requestDTO = call.receive<NewVurderingRequestDTO>()

            validateVeilederAccess(
                action = "POST /vurderinger",
                personident = requestDTO.personident,
                veilederTilgangskontrollClient = veilederTilgangskontrollClient,
            ) {
                val callId = call.getCallId()
                if (requestDTO.document.isEmpty()) {
                    throw IllegalArgumentException("Vurdering can't have empty document, callId: $callId")
                }

                val newVurdering = vurderingService.createNewVurdering(
                    personident = requestDTO.personident,
                    veilederident = call.getNAVIdent(),
                    vurderingType = requestDTO.vurderingType,
                    begrunnelse = requestDTO.begrunnelse,
                    document = requestDTO.document,
                    varselSvarfrist = requestDTO.varselSvarfrist,
                    callId = callId,
                )

                call.respond(HttpStatusCode.Created, NewVurderingResponseDTO.fromVurdering(newVurdering))
            }
        }

        post("/get-vurderinger") {
            // TODO: Implement
            call.respond(HttpStatusCode.NotImplemented)
        }
    }
}
