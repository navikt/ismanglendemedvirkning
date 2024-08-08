package no.nav.syfo.api

import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.syfo.UserConstants
import no.nav.syfo.infrastructure.NAV_PERSONIDENT_HEADER
import no.nav.syfo.infrastructure.bearerHeader
import org.amshove.kluent.shouldBeEqualTo

fun TestApplicationEngine.testMissingToken(url: String, httpMethod: HttpMethod) {
    with(
        handleRequest(httpMethod, url) {}

    ) {
        response.status() shouldBeEqualTo HttpStatusCode.Unauthorized
    }
}

fun TestApplicationEngine.testDeniedPersonAccess(
    url: String,
    validToken: String,
    requestBody: String,
    httpMethod: HttpMethod,
) {
    with(
        handleRequest(httpMethod, url) {
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
            addHeader(
                NAV_PERSONIDENT_HEADER,
                UserConstants.ARBEIDSTAKER_PERSONIDENT_VEILEDER_NO_ACCESS.value
            )
            setBody(requestBody)
        }
    ) {
        response.status() shouldBeEqualTo HttpStatusCode.Forbidden
    }
}
