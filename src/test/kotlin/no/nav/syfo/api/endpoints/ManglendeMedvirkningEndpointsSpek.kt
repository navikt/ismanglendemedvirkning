package no.nav.syfo.api.endpoints

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_VEILEDER_NO_ACCESS
import no.nav.syfo.UserConstants.VEILEDER_IDENT
import no.nav.syfo.api.generateJWT
import no.nav.syfo.api.model.NewVurderingRequestDTO
import no.nav.syfo.api.model.NewVurderingResponseDTO
import no.nav.syfo.api.testApiModule
import no.nav.syfo.api.testDeniedPersonAccess
import no.nav.syfo.api.testMissingToken
import no.nav.syfo.domain.VurderingType
import no.nav.syfo.generator.generateDocumentComponent
import no.nav.syfo.infrastructure.NAV_PERSONIDENT_HEADER
import no.nav.syfo.infrastructure.bearerHeader
import no.nav.syfo.infrastructure.database.dropData
import no.nav.syfo.util.configuredJacksonMapper
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate

object ManglendeMedvirkningEndpointsSpek : Spek({

    val objectMapper: ObjectMapper = configuredJacksonMapper()
    val urlVurderinger = "/api/internad/v1/manglende-medvirkning"

    describe(ManglendeMedvirkningEndpointsSpek::class.java.simpleName) {
        with(TestApplicationEngine()) {
            start()
            val externalMockEnvironment = ExternalMockEnvironment.instance
            val database = externalMockEnvironment.database

            application.testApiModule(
                externalMockEnvironment = externalMockEnvironment,
            )

            val validToken = generateJWT(
                audience = externalMockEnvironment.environment.azure.appClientId,
                issuer = externalMockEnvironment.wellKnownInternalAzureAD.issuer,
                navIdent = VEILEDER_IDENT,
            )

            val personIdent = ARBEIDSTAKER_PERSONIDENT.value

            beforeEachTest { database.dropData() }

            describe("POST /vurderinger") {
                val begrunnelse = "Dette er en begrunnelse for vurdering av 8-8"

                it("Successfully creates a new forhandsvarsel with varsel and pdf") {
                    val forhandsvarselRequestDTO = NewVurderingRequestDTO(
                        personident = ARBEIDSTAKER_PERSONIDENT,
                        vurderingType = VurderingType.FORHANDSVARSEL,
                        begrunnelse = "Fin begrunnelse",
                        document = generateDocumentComponent(
                            fritekst = begrunnelse,
                            header = "Forhåndsvarsel"
                        ),
                        varselSvarfrist = LocalDate.now().plusDays(14),
                    )
                    with(
                        handleRequest(HttpMethod.Post, "$urlVurderinger/vurderinger") {
                            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            setBody(objectMapper.writeValueAsString(forhandsvarselRequestDTO))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.Created
                        val responseDTO = objectMapper.readValue<NewVurderingResponseDTO>(response.content!!)
                        responseDTO.personident shouldBeEqualTo forhandsvarselRequestDTO.personident
                        responseDTO.vurderingType shouldBeEqualTo forhandsvarselRequestDTO.vurderingType
                        responseDTO.begrunnelse shouldBeEqualTo forhandsvarselRequestDTO.begrunnelse
                        responseDTO.document shouldBeEqualTo forhandsvarselRequestDTO.document
                        responseDTO.varsel?.svarfrist shouldBeEqualTo forhandsvarselRequestDTO.varselSvarfrist
                    }
                }

                it("Successfully creates an STANS vurdering") {
                    val avslagVurdering = NewVurderingRequestDTO(
                        personident = ARBEIDSTAKER_PERSONIDENT,
                        vurderingType = VurderingType.STANS,
                        begrunnelse = "Fin begrunnelse",
                        document = generateDocumentComponent(
                            fritekst = begrunnelse,
                            header = "Stans"
                        ),
                        varselSvarfrist = null,
                    )
                    with(
                        handleRequest(HttpMethod.Post, "$urlVurderinger/vurderinger") {
                            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            setBody(objectMapper.writeValueAsString(avslagVurdering))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.Created
                        val responseDTO = objectMapper.readValue<NewVurderingResponseDTO>(response.content!!)
                        responseDTO.personident shouldBeEqualTo avslagVurdering.personident
                        responseDTO.vurderingType shouldBeEqualTo avslagVurdering.vurderingType
                        responseDTO.begrunnelse shouldBeEqualTo avslagVurdering.begrunnelse
                        responseDTO.document shouldBeEqualTo avslagVurdering.document
                        responseDTO.varsel?.svarfrist shouldBeEqualTo null
                    }
                }

                it("Fails to create a new forhandsvarsel when document is missing") {
                    val forhandsvarselRequestDTO = NewVurderingRequestDTO(
                        personident = ARBEIDSTAKER_PERSONIDENT,
                        vurderingType = VurderingType.FORHANDSVARSEL,
                        begrunnelse = "Fin begrunnelse",
                        document = emptyList(),
                        varselSvarfrist = LocalDate.now().plusDays(14),
                    )
                    with(
                        handleRequest(HttpMethod.Post, "$urlVurderinger/vurderinger") {
                            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            setBody(objectMapper.writeValueAsString(forhandsvarselRequestDTO))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.BadRequest
                    }
                }

                it("Returns status Unauthorized if no token is supplied") {
                    testMissingToken("$urlVurderinger/vurderinger", HttpMethod.Post)
                }
                it("returns status Forbidden if denied access to person") {
                    val forhandsvarselRequestDTO = NewVurderingRequestDTO(
                        personident = ARBEIDSTAKER_PERSONIDENT_VEILEDER_NO_ACCESS,
                        vurderingType = VurderingType.FORHANDSVARSEL,
                        begrunnelse = "Fin begrunnelse",
                        document = emptyList(),
                        varselSvarfrist = LocalDate.now().plusDays(14),
                    )
                    testDeniedPersonAccess(
                        "$urlVurderinger/vurderinger",
                        validToken,
                        objectMapper.writeValueAsString(forhandsvarselRequestDTO),
                        HttpMethod.Post
                    )
                }
            }

            describe("GET /vurderinger") {
                it("Successfully get vurdering") {
                    with(
                        handleRequest(HttpMethod.Get, "$urlVurderinger/vurderinger") {
                            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            addHeader(NAV_PERSONIDENT_HEADER, personIdent)
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.NotImplemented
                    }
                }
            }

            describe("POST /get-vurderinger") {
                it("Successfully retrieves a group of vurderinger") {
                    with(
                        handleRequest(HttpMethod.Post, "$urlVurderinger/get-vurderinger") {
                            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.NotImplemented
                    }
                }
            }
        }
    }
})
