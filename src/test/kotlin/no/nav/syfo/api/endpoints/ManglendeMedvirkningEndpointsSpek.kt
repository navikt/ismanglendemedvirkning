package no.nav.syfo.api.endpoints

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT
import no.nav.syfo.UserConstants.ARBEIDSTAKER_PERSONIDENT_VEILEDER_NO_ACCESS
import no.nav.syfo.UserConstants.PDF_FORHANDSVARSEL
import no.nav.syfo.UserConstants.PDF_STANS
import no.nav.syfo.UserConstants.PDF_VURDERING
import no.nav.syfo.UserConstants.VEILEDER_IDENT
import no.nav.syfo.api.generateJWT
import no.nav.syfo.api.model.*
import no.nav.syfo.api.testApiModule
import no.nav.syfo.api.testDeniedPersonAccess
import no.nav.syfo.api.testMissingToken
import no.nav.syfo.application.model.NewVurderingRequestDTO
import no.nav.syfo.domain.Personident
import no.nav.syfo.domain.VurderingType
import no.nav.syfo.generator.generateDocumentComponent
import no.nav.syfo.generator.generateVurdering
import no.nav.syfo.infrastructure.NAV_PERSONIDENT_HEADER
import no.nav.syfo.infrastructure.bearerHeader
import no.nav.syfo.infrastructure.database.dropData
import no.nav.syfo.infrastructure.database.getVurderingPdf
import no.nav.syfo.infrastructure.database.repository.VurderingRepository
import no.nav.syfo.util.configuredJacksonMapper
import org.amshove.kluent.shouldBe
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
            val vurderingRepository = VurderingRepository(database)

            application.testApiModule(
                externalMockEnvironment = externalMockEnvironment,
            )

            val validToken = generateJWT(
                audience = externalMockEnvironment.environment.azure.appClientId,
                issuer = externalMockEnvironment.wellKnownInternalAzureAD.issuer,
                navIdent = VEILEDER_IDENT,
            )

            val personIdent = ARBEIDSTAKER_PERSONIDENT.value
            val personIdentNoAccess = ARBEIDSTAKER_PERSONIDENT_VEILEDER_NO_ACCESS.value

            beforeEachTest { database.dropData() }

            describe("POST /vurderinger") {
                val begrunnelse = "Dette er en begrunnelse for vurdering av 8-8"

                it("Successfully creates a new forhandsvarsel with varsel and pdf") {
                    val forhandsvarselRequestDTO = NewVurderingRequestDTO.Forhandsvarsel(
                        personident = ARBEIDSTAKER_PERSONIDENT.value,
                        begrunnelse = "Fin begrunnelse",
                        document = generateDocumentComponent(
                            fritekst = begrunnelse,
                            header = "Forhåndsvarsel"
                        ),
                        varselSvarfrist = LocalDate.now().plusDays(14),
                    )
                    val json = objectMapper.writeValueAsString(forhandsvarselRequestDTO)
                    with(
                        handleRequest(HttpMethod.Post, "$urlVurderinger/vurderinger") {
                            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            setBody(objectMapper.writeValueAsString(forhandsvarselRequestDTO))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.Created
                        val responseDTO = objectMapper.readValue<VurderingResponseDTO>(response.content!!)
                        responseDTO.personident shouldBeEqualTo forhandsvarselRequestDTO.personident
                        responseDTO.vurderingType shouldBeEqualTo VurderingType.FORHANDSVARSEL
                        responseDTO.begrunnelse shouldBeEqualTo forhandsvarselRequestDTO.begrunnelse
                        responseDTO.veilederident shouldBeEqualTo VEILEDER_IDENT.value
                        responseDTO.document shouldBeEqualTo forhandsvarselRequestDTO.document
                        responseDTO.varsel?.svarfrist shouldBeEqualTo forhandsvarselRequestDTO.varselSvarfrist

                        val pVurderingPdf = database.getVurderingPdf(responseDTO.uuid)
                        pVurderingPdf?.pdf?.size shouldBeEqualTo PDF_FORHANDSVARSEL.size
                        pVurderingPdf?.pdf?.get(0) shouldBeEqualTo PDF_FORHANDSVARSEL[0]
                        pVurderingPdf?.pdf?.get(1) shouldBeEqualTo PDF_FORHANDSVARSEL[1]
                    }
                }

                it("Successfully creates an STANS vurdering with pdf") {
                    val stansVurdering = NewVurderingRequestDTO.Stans(
                        personident = ARBEIDSTAKER_PERSONIDENT.value,
                        begrunnelse = "Fin begrunnelse",
                        document = generateDocumentComponent(
                            fritekst = begrunnelse,
                            header = "Stans"
                        ),
                    )
                    val json = objectMapper.writeValueAsString(stansVurdering)
                    with(
                        handleRequest(HttpMethod.Post, "$urlVurderinger/vurderinger") {
                            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            setBody(objectMapper.writeValueAsString(stansVurdering))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.Created
                        val responseDTO = objectMapper.readValue<VurderingResponseDTO>(response.content!!)
                        responseDTO.personident shouldBeEqualTo stansVurdering.personident
                        responseDTO.vurderingType shouldBeEqualTo VurderingType.STANS
                        responseDTO.begrunnelse shouldBeEqualTo stansVurdering.begrunnelse
                        responseDTO.veilederident shouldBeEqualTo VEILEDER_IDENT.value
                        responseDTO.document shouldBeEqualTo stansVurdering.document
                        responseDTO.varsel?.svarfrist shouldBeEqualTo null

                        val pVurderingPdf = database.getVurderingPdf(responseDTO.uuid)
                        pVurderingPdf?.pdf?.size shouldBeEqualTo PDF_STANS.size
                        pVurderingPdf?.pdf?.get(0) shouldBeEqualTo PDF_STANS[0]
                        pVurderingPdf?.pdf?.get(1) shouldBeEqualTo PDF_STANS[1]
                    }
                }

                it("Successfully creates an OPPFYLT vurdering with pdf") {
                    val oppfyltVurdering = NewVurderingRequestDTO.Oppfylt(
                        personident = ARBEIDSTAKER_PERSONIDENT.value,
                        begrunnelse = "Fin begrunnelse",
                        document = generateDocumentComponent(
                            fritekst = begrunnelse,
                            header = "Oppfylt"
                        ),
                    )
                    with(
                        handleRequest(HttpMethod.Post, "$urlVurderinger/vurderinger") {
                            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            setBody(objectMapper.writeValueAsString(oppfyltVurdering))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.Created
                        val responseDTO = objectMapper.readValue<VurderingResponseDTO>(response.content!!)
                        responseDTO.personident shouldBeEqualTo oppfyltVurdering.personident
                        responseDTO.vurderingType shouldBeEqualTo VurderingType.OPPFYLT
                        responseDTO.begrunnelse shouldBeEqualTo oppfyltVurdering.begrunnelse
                        responseDTO.veilederident shouldBeEqualTo VEILEDER_IDENT.value
                        responseDTO.document shouldBeEqualTo oppfyltVurdering.document
                        responseDTO.varsel?.svarfrist shouldBeEqualTo null

                        val pVurderingPdf = database.getVurderingPdf(responseDTO.uuid)
                        pVurderingPdf?.pdf?.size shouldBeEqualTo PDF_VURDERING.size
                        pVurderingPdf?.pdf?.get(0) shouldBeEqualTo PDF_VURDERING[0]
                        pVurderingPdf?.pdf?.get(1) shouldBeEqualTo PDF_VURDERING[1]
                    }
                }

                it("Successfully creates an UNNTAK vurdering with pdf") {
                    val unntakVurdering = NewVurderingRequestDTO.Unntak(
                        personident = ARBEIDSTAKER_PERSONIDENT.value,
                        begrunnelse = "Unntak fordi...",
                        document = generateDocumentComponent(
                            fritekst = begrunnelse,
                            header = "Unntak"
                        ),
                    )
                    with(
                        handleRequest(HttpMethod.Post, "$urlVurderinger/vurderinger") {
                            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            setBody(objectMapper.writeValueAsString(unntakVurdering))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.Created
                        val responseDTO = objectMapper.readValue<VurderingResponseDTO>(response.content!!)
                        responseDTO.personident shouldBeEqualTo unntakVurdering.personident
                        responseDTO.vurderingType shouldBeEqualTo VurderingType.UNNTAK
                        responseDTO.begrunnelse shouldBeEqualTo unntakVurdering.begrunnelse
                        responseDTO.veilederident shouldBeEqualTo VEILEDER_IDENT.value
                        responseDTO.document shouldBeEqualTo unntakVurdering.document
                        responseDTO.varsel?.svarfrist shouldBeEqualTo null

                        val pVurderingPdf = database.getVurderingPdf(responseDTO.uuid)
                        pVurderingPdf?.pdf?.size shouldBeEqualTo PDF_VURDERING.size
                        pVurderingPdf?.pdf?.get(0) shouldBeEqualTo PDF_VURDERING[0]
                        pVurderingPdf?.pdf?.get(1) shouldBeEqualTo PDF_VURDERING[1]
                    }
                }

                it("Successfully creates an IKKE AKTUELL vurdering with pdf") {
                    val ikkeAktuellVurdering = NewVurderingRequestDTO.IkkeAktuell(
                        personident = ARBEIDSTAKER_PERSONIDENT.value,
                        begrunnelse = "Fin begrunnelse",
                        document = generateDocumentComponent(
                            fritekst = begrunnelse,
                            header = "Oppfylt"
                        ),
                    )
                    with(
                        handleRequest(HttpMethod.Post, "$urlVurderinger/vurderinger") {
                            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            setBody(objectMapper.writeValueAsString(ikkeAktuellVurdering))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.Created
                        val responseDTO = objectMapper.readValue<VurderingResponseDTO>(response.content!!)
                        responseDTO.personident shouldBeEqualTo ikkeAktuellVurdering.personident
                        responseDTO.vurderingType shouldBeEqualTo VurderingType.IKKE_AKTUELL
                        responseDTO.begrunnelse shouldBeEqualTo ikkeAktuellVurdering.begrunnelse
                        responseDTO.veilederident shouldBeEqualTo VEILEDER_IDENT.value
                        responseDTO.document shouldBeEqualTo ikkeAktuellVurdering.document
                        responseDTO.varsel?.svarfrist shouldBeEqualTo null

                        val pVurderingPdf = database.getVurderingPdf(responseDTO.uuid)
                        pVurderingPdf?.pdf?.size shouldBeEqualTo PDF_VURDERING.size
                        pVurderingPdf?.pdf?.get(0) shouldBeEqualTo PDF_VURDERING[0]
                        pVurderingPdf?.pdf?.get(1) shouldBeEqualTo PDF_VURDERING[1]
                    }
                }

                it("Fails to create a new forhandsvarsel when document is missing") {
                    val forhandsvarselRequestDTO = NewVurderingRequestDTO.Forhandsvarsel(
                        personident = ARBEIDSTAKER_PERSONIDENT.value,
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
                    val forhandsvarselRequestDTO = NewVurderingRequestDTO.Forhandsvarsel(
                        personident = ARBEIDSTAKER_PERSONIDENT_VEILEDER_NO_ACCESS.value,
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
                it("Successfully get vurdering (emptyList)") {
                    with(
                        handleRequest(HttpMethod.Get, "$urlVurderinger/vurderinger") {
                            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            addHeader(NAV_PERSONIDENT_HEADER, personIdent)
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.NoContent
                    }
                }
                it("Successfully get vurdering (single vurdering)") {
                    val vurdering = generateVurdering(
                        personident = Personident(personIdent),
                        type = VurderingType.FORHANDSVARSEL,
                    )
                    vurderingRepository.saveManglendeMedvirkningVurdering(
                        vurdering = vurdering,
                        vurderingPdf = PDF_FORHANDSVARSEL,
                    )
                    with(
                        handleRequest(HttpMethod.Get, "$urlVurderinger/vurderinger") {
                            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            addHeader(NAV_PERSONIDENT_HEADER, personIdent)
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                        val responseDTOList = objectMapper.readValue<List<VurderingResponseDTO>>(response.content!!)
                        responseDTOList.size shouldBeEqualTo 1
                        val vurderingResponseDTO = responseDTOList[0]
                        vurderingResponseDTO.vurderingType shouldBeEqualTo VurderingType.FORHANDSVARSEL
                        vurderingResponseDTO.personident shouldBeEqualTo personIdent
                        vurderingResponseDTO.begrunnelse shouldBeEqualTo vurdering.begrunnelse
                    }
                }
                it("Successfully get vurdering (two vurderinger)") {
                    val forhandsvarsel = generateVurdering(
                        personident = Personident(personIdent),
                        type = VurderingType.FORHANDSVARSEL,
                    )
                    val oppfylt = generateVurdering(
                        personident = Personident(personIdent),
                        type = VurderingType.OPPFYLT,
                        createdAt = forhandsvarsel.createdAt.plusSeconds(1),
                    )
                    vurderingRepository.saveManglendeMedvirkningVurdering(
                        vurdering = forhandsvarsel,
                        vurderingPdf = PDF_FORHANDSVARSEL,
                    )
                    vurderingRepository.saveManglendeMedvirkningVurdering(
                        vurdering = oppfylt,
                        vurderingPdf = PDF_VURDERING,
                    )
                    with(
                        handleRequest(HttpMethod.Get, "$urlVurderinger/vurderinger") {
                            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            addHeader(NAV_PERSONIDENT_HEADER, personIdent)
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                        val responseDTOList = objectMapper.readValue<List<VurderingResponseDTO>>(response.content!!)
                        responseDTOList.size shouldBeEqualTo 2
                        responseDTOList[0].vurderingType shouldBeEqualTo VurderingType.OPPFYLT
                        responseDTOList[1].vurderingType shouldBeEqualTo VurderingType.FORHANDSVARSEL
                    }
                }
                it("Fails get vurdering when no access") {
                    val vurdering = generateVurdering(
                        personident = Personident(personIdentNoAccess),
                        type = VurderingType.FORHANDSVARSEL,
                    )
                    vurderingRepository.saveManglendeMedvirkningVurdering(
                        vurdering = vurdering,
                        vurderingPdf = PDF_FORHANDSVARSEL,
                    )
                    with(
                        handleRequest(HttpMethod.Get, "$urlVurderinger/vurderinger") {
                            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            addHeader(NAV_PERSONIDENT_HEADER, personIdentNoAccess)
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.Forbidden
                    }
                }
            }

            describe("POST /get-vurderinger") {
                val personidenter = listOf(personIdent)
                val requestDTO = VurderingerRequestDTO(personidenter)
                it("Successfully retrieves an empty group of vurderinger") {
                    with(
                        handleRequest(HttpMethod.Post, "$urlVurderinger/get-vurderinger") {
                            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            setBody(objectMapper.writeValueAsString(requestDTO))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.NoContent
                    }
                }
                it("Successfully retrieves a group of vurderinger") {
                    val forhandsvarsel = generateVurdering(
                        personident = Personident(personIdent),
                        type = VurderingType.FORHANDSVARSEL,
                    )
                    vurderingRepository.saveManglendeMedvirkningVurdering(
                        vurdering = forhandsvarsel,
                        vurderingPdf = PDF_FORHANDSVARSEL,
                    )
                    with(
                        handleRequest(HttpMethod.Post, "$urlVurderinger/get-vurderinger") {
                            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            setBody(objectMapper.writeValueAsString(requestDTO))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                        val responseDTO = objectMapper.readValue<VurderingerResponseDTO>(response.content!!)
                        val vurderingResponseDTO = responseDTO.vurderinger.get(personIdent)
                        vurderingResponseDTO!!.personident shouldBeEqualTo personIdent
                        vurderingResponseDTO.vurderingType shouldBeEqualTo VurderingType.FORHANDSVARSEL
                    }
                }
                it("Retrieves latest vurderinger if more than one") {
                    val forhandsvarsel = generateVurdering(
                        personident = Personident(personIdent),
                        type = VurderingType.FORHANDSVARSEL,
                    )
                    vurderingRepository.saveManglendeMedvirkningVurdering(
                        vurdering = forhandsvarsel,
                        vurderingPdf = PDF_FORHANDSVARSEL,
                    )
                    val oppfylt = generateVurdering(
                        personident = Personident(personIdent),
                        type = VurderingType.OPPFYLT,
                        createdAt = forhandsvarsel.createdAt.plusSeconds(1),
                    )
                    vurderingRepository.saveManglendeMedvirkningVurdering(
                        vurdering = oppfylt,
                        vurderingPdf = PDF_FORHANDSVARSEL,
                    )
                    with(
                        handleRequest(HttpMethod.Post, "$urlVurderinger/get-vurderinger") {
                            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            setBody(objectMapper.writeValueAsString(requestDTO))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                        val responseDTO = objectMapper.readValue<VurderingerResponseDTO>(response.content!!)
                        val vurderingResponseDTO = responseDTO.vurderinger.get(personIdent)
                        vurderingResponseDTO!!.personident shouldBeEqualTo personIdent
                        vurderingResponseDTO.vurderingType shouldBeEqualTo VurderingType.OPPFYLT
                    }
                }
                it("Only retrieves vurderinger for which the user has access") {
                    val forhandsvarsel = generateVurdering(
                        personident = Personident(personIdent),
                        type = VurderingType.FORHANDSVARSEL,
                    )
                    val forhandsvarselNoAccess = generateVurdering(
                        personident = Personident(personIdentNoAccess),
                        type = VurderingType.FORHANDSVARSEL,
                    )
                    vurderingRepository.saveManglendeMedvirkningVurdering(
                        vurdering = forhandsvarselNoAccess,
                        vurderingPdf = PDF_FORHANDSVARSEL,
                    )
                    vurderingRepository.saveManglendeMedvirkningVurdering(
                        vurdering = forhandsvarsel,
                        vurderingPdf = PDF_FORHANDSVARSEL,
                    )
                    with(
                        handleRequest(HttpMethod.Post, "$urlVurderinger/get-vurderinger") {
                            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            setBody(objectMapper.writeValueAsString(VurderingerRequestDTO(listOf(personIdent, personIdentNoAccess))))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                        val responseDTO = objectMapper.readValue<VurderingerResponseDTO>(response.content!!)
                        responseDTO.vurderinger.keys.contains(personIdent) shouldBe true
                        responseDTO.vurderinger.keys.contains(personIdentNoAccess) shouldBe false
                    }
                }
            }
        }
    }
})
