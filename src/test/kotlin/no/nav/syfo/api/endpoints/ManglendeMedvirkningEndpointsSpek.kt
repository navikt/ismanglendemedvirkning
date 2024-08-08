package no.nav.syfo.api.endpoints

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.*
import io.ktor.server.testing.TestApplicationEngine
import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT
import no.nav.syfo.UserConstants.VEILEDER_IDENT
import no.nav.syfo.api.generateJWT
import no.nav.syfo.api.model.NewVurderingRequestDTO
import no.nav.syfo.api.model.NewVurderingResponseDTO
import no.nav.syfo.api.testApiModule
import no.nav.syfo.application.VurderingService
import no.nav.syfo.domain.VurderingType
import no.nav.syfo.generator.generateDocumentComponent
import no.nav.syfo.infrastructure.NAV_PERSONIDENT_HEADER
import no.nav.syfo.infrastructure.bearerHeader
import no.nav.syfo.infrastructure.database.dropData
import no.nav.syfo.infrastructure.database.repository.VurderingRepository
import no.nav.syfo.util.configuredJacksonMapper
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate

object ManglendeMedvirkningEndpointsSpek : Spek({

    val objectMapper: ObjectMapper = configuredJacksonMapper()
    val urlVurderinger = "/api/internad/v1"

    describe(ManglendeMedvirkningEndpointsSpek::class.java.simpleName) {
        with(TestApplicationEngine()) {
            start()
            val externalMockEnvironment = ExternalMockEnvironment.instance
            val database = externalMockEnvironment.database

            application.testApiModule(
                externalMockEnvironment = externalMockEnvironment,
            )

            val vurderingRepository = VurderingRepository(database)
            val vurderingService = VurderingService(vurderingRepository = vurderingRepository)
            val validToken = generateJWT(
                audience = externalMockEnvironment.environment.azure.appClientId,
                issuer = externalMockEnvironment.wellKnownInternalAzureAD.issuer,
                navIdent = VEILEDER_IDENT,
            )

            val personIdent = ARBEIDSTAKER_PERSONIDENT.value

            beforeEachTest { database.dropData() }

            describe("POST /vurderinger") {
                val begrunnelse = "Dette er en begrunnelse for vurdering av 8-4"
                val vurderingDocumentAvslag = generateDocumentComponent(
                    fritekst = begrunnelse,
                    header = "Avslag",
                )
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
                val vurderingRequestDTO = NewVurderingRequestDTO(
                    personident = ARBEIDSTAKER_PERSONIDENT,
                    vurderingType = VurderingType.OPPFYLT,
                    begrunnelse = begrunnelse,
                    document = generateDocumentComponent(
                        fritekst = begrunnelse,
                        header = "Oppfylt",
                    ),
                    varselSvarfrist = null,
                )

                it("Successfully creates a new forhandsvarsel with varsel and pdf") {
                    with(
                        handleRequest(HttpMethod.Post, "$urlVurderinger/vurderinger") {
                            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            addHeader(NAV_PERSONIDENT_HEADER, personIdent)
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
                            addHeader(NAV_PERSONIDENT_HEADER, personIdent)
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.NotImplemented
                    }
                }
            }
        }
    }
})
