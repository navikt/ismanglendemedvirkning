package no.nav.syfo.api.endpoints

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
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
import no.nav.syfo.application.model.NewVurderingRequestDTO
import no.nav.syfo.domain.Personident
import no.nav.syfo.domain.VurderingType
import no.nav.syfo.generator.generateDocumentComponent
import no.nav.syfo.generator.generateVurdering
import no.nav.syfo.infrastructure.NAV_PERSONIDENT_HEADER
import no.nav.syfo.infrastructure.bearerHeader
import no.nav.syfo.infrastructure.database.getVurderingPdf
import no.nav.syfo.infrastructure.database.repository.VurderingRepository
import no.nav.syfo.util.configure
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import java.time.LocalDate

class ManglendeMedvirkningEndpointsTest {

    private val urlVurderinger = "/api/internad/v1/manglende-medvirkning"

    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val vurderingRepository = VurderingRepository(database)

    private fun ApplicationTestBuilder.setupApiAndClient(): HttpClient {
        application {
            testApiModule(
                externalMockEnvironment = ExternalMockEnvironment.instance,
            )
        }
        val client = createClient {
            install(ContentNegotiation) {
                jackson { configure() }
            }
        }

        return client
    }

    private val validToken = generateJWT(
        audience = externalMockEnvironment.environment.azure.appClientId,
        issuer = externalMockEnvironment.wellKnownInternalAzureAD.issuer,
        navIdent = VEILEDER_IDENT,
    )

    @BeforeEach
    fun beforeEach() {
        database.resetDatabase()
    }

    @Nested
    @DisplayName("POST /vurderinger")
    inner class PostVurderinger {
        private val begrunnelse = "Dette er en begrunnelse for vurdering av 8-8"

        @Test
        fun `Successfully creates a new forhandsvarsel with varsel and pdf`() {
            val forhandsvarselRequestDTO = NewVurderingRequestDTO.Forhandsvarsel(
                personident = ARBEIDSTAKER_PERSONIDENT.value,
                begrunnelse = "Fin begrunnelse",
                document = generateDocumentComponent(
                    fritekst = begrunnelse,
                    header = "Forhåndsvarsel"
                ),
                varselSvarfrist = LocalDate.now().plusDays(14),
            )

            testApplication {
                val client = setupApiAndClient()
                val response = client.post("$urlVurderinger/vurderinger") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(validToken)
                    setBody(forhandsvarselRequestDTO)
                }

                assertEquals(HttpStatusCode.Created, response.status)
                val responseDTO = response.body<VurderingResponseDTO>()
                assertEquals(forhandsvarselRequestDTO.personident, responseDTO.personident)
                assertEquals(VurderingType.FORHANDSVARSEL, responseDTO.vurderingType)
                assertEquals(forhandsvarselRequestDTO.begrunnelse, responseDTO.begrunnelse)
                assertEquals(VEILEDER_IDENT.value, responseDTO.veilederident)
                assertEquals(forhandsvarselRequestDTO.document, responseDTO.document)
                assertEquals(forhandsvarselRequestDTO.varselSvarfrist, responseDTO.varsel?.svarfrist)

                val pVurderingPdf = database.getVurderingPdf(responseDTO.uuid)
                assertEquals(PDF_FORHANDSVARSEL.size, pVurderingPdf?.pdf?.size)
                assertEquals(PDF_FORHANDSVARSEL[0], pVurderingPdf?.pdf?.get(0))
                assertEquals(PDF_FORHANDSVARSEL[1], pVurderingPdf?.pdf?.get(1))
            }
        }

        @Test
        fun `Successfully creates a STANS vurdering with pdf`() {
            val stansVurdering = NewVurderingRequestDTO.Stans(
                personident = ARBEIDSTAKER_PERSONIDENT.value,
                begrunnelse = "Fin begrunnelse",
                stansdato = LocalDate.now().plusDays(5),
                document = generateDocumentComponent(
                    fritekst = begrunnelse,
                    header = "Stans"
                ),
            )
            testApplication {
                val client = setupApiAndClient()
                val response = client.post("$urlVurderinger/vurderinger") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(validToken)
                    setBody(stansVurdering)
                }

                assertEquals(HttpStatusCode.Created, response.status)
                val responseDTO = response.body<VurderingResponseDTO>()
                assertEquals(stansVurdering.personident, responseDTO.personident)
                assertEquals(VurderingType.STANS, responseDTO.vurderingType)
                assertEquals(stansVurdering.begrunnelse, responseDTO.begrunnelse)
                assertEquals(stansVurdering.stansdato, responseDTO.stansdato)
                assertEquals(VEILEDER_IDENT.value, responseDTO.veilederident)
                assertEquals(stansVurdering.document, responseDTO.document)
                assertEquals(null, responseDTO.varsel?.svarfrist)

                val pVurderingPdf = database.getVurderingPdf(responseDTO.uuid)
                assertEquals(PDF_STANS.size, pVurderingPdf?.pdf?.size)
                assertEquals(PDF_STANS[0], pVurderingPdf?.pdf?.get(0))
                assertEquals(PDF_STANS[1], pVurderingPdf?.pdf?.get(1))
            }
        }

        @Test
        fun `Successfully creates an OPPFYLT vurdering with pdf`() {
            val oppfyltVurdering = NewVurderingRequestDTO.Oppfylt(
                personident = ARBEIDSTAKER_PERSONIDENT.value,
                begrunnelse = "Fin begrunnelse",
                document = generateDocumentComponent(
                    fritekst = begrunnelse,
                    header = "Oppfylt"
                ),
            )

            testApplication {
                val client = setupApiAndClient()
                val response = client.post("$urlVurderinger/vurderinger") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(validToken)
                    setBody(oppfyltVurdering)
                }

                assertEquals(HttpStatusCode.Created, response.status)
                val responseDTO = response.body<VurderingResponseDTO>()
                assertEquals(oppfyltVurdering.personident, responseDTO.personident)
                assertEquals(VurderingType.OPPFYLT, responseDTO.vurderingType)
                assertEquals(oppfyltVurdering.begrunnelse, responseDTO.begrunnelse)
                assertEquals(VEILEDER_IDENT.value, responseDTO.veilederident)
                assertEquals(oppfyltVurdering.document, responseDTO.document)
                assertEquals(null, responseDTO.varsel?.svarfrist)

                val pVurderingPdf = database.getVurderingPdf(responseDTO.uuid)
                assertEquals(PDF_VURDERING.size, pVurderingPdf?.pdf?.size)
                assertEquals(PDF_VURDERING[0], pVurderingPdf?.pdf?.get(0))
                assertEquals(PDF_VURDERING[1], pVurderingPdf?.pdf?.get(1))
            }
        }

        @Test
        fun `Successfully creates an UNNTAK vurdering with pdf`() {
            val unntakVurdering = NewVurderingRequestDTO.Unntak(
                personident = ARBEIDSTAKER_PERSONIDENT.value,
                begrunnelse = "Unntak fordi...",
                document = generateDocumentComponent(
                    fritekst = begrunnelse,
                    header = "Unntak"
                ),
            )

            testApplication {
                val client = setupApiAndClient()
                val response = client.post("$urlVurderinger/vurderinger") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(validToken)
                    setBody(unntakVurdering)
                }

                assertEquals(HttpStatusCode.Created, response.status)
                val responseDTO = response.body<VurderingResponseDTO>()
                assertEquals(unntakVurdering.personident, responseDTO.personident)
                assertEquals(VurderingType.UNNTAK, responseDTO.vurderingType)
                assertEquals(unntakVurdering.begrunnelse, responseDTO.begrunnelse)
                assertEquals(VEILEDER_IDENT.value, responseDTO.veilederident)
                assertEquals(unntakVurdering.document, responseDTO.document)
                assertEquals(null, responseDTO.varsel?.svarfrist)

                val pVurderingPdf = database.getVurderingPdf(responseDTO.uuid)
                assertEquals(PDF_VURDERING.size, pVurderingPdf?.pdf?.size)
                assertEquals(PDF_VURDERING[0], pVurderingPdf?.pdf?.get(0))
                assertEquals(PDF_VURDERING[1], pVurderingPdf?.pdf?.get(1))
            }
        }

        @Test
        fun `Successfully creates an IKKE AKTUELL vurdering with pdf`() {
            val ikkeAktuellVurdering = NewVurderingRequestDTO.IkkeAktuell(
                personident = ARBEIDSTAKER_PERSONIDENT.value,
                begrunnelse = "Fin begrunnelse",
                document = generateDocumentComponent(
                    fritekst = begrunnelse,
                    header = "Oppfylt"
                ),
            )
            testApplication {
                val client = setupApiAndClient()
                val response = client.post("$urlVurderinger/vurderinger") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(validToken)
                    setBody(ikkeAktuellVurdering)
                }

                assertEquals(HttpStatusCode.Created, response.status)
                val responseDTO = response.body<VurderingResponseDTO>()
                assertEquals(ikkeAktuellVurdering.personident, responseDTO.personident)
                assertEquals(VurderingType.IKKE_AKTUELL, responseDTO.vurderingType)
                assertEquals(ikkeAktuellVurdering.begrunnelse, responseDTO.begrunnelse)
                assertEquals(VEILEDER_IDENT.value, responseDTO.veilederident)
                assertEquals(ikkeAktuellVurdering.document, responseDTO.document)
                assertEquals(null, responseDTO.varsel?.svarfrist)

                val pVurderingPdf = database.getVurderingPdf(responseDTO.uuid)
                assertEquals(PDF_VURDERING.size, pVurderingPdf?.pdf?.size)
                assertEquals(PDF_VURDERING[0], pVurderingPdf?.pdf?.get(0))
                assertEquals(PDF_VURDERING[1], pVurderingPdf?.pdf?.get(1))
            }
        }

        @Test
        fun `Fails to create a new forhandsvarsel when document is missing`() {
            val forhandsvarselRequestDTO = NewVurderingRequestDTO.Forhandsvarsel(
                personident = ARBEIDSTAKER_PERSONIDENT.value,
                begrunnelse = "Fin begrunnelse",
                document = emptyList(),
                varselSvarfrist = LocalDate.now().plusDays(14),
            )

            testApplication {
                val client = setupApiAndClient()
                val response = client.post("$urlVurderinger/vurderinger") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(validToken)
                    setBody(forhandsvarselRequestDTO)
                }

                assertEquals(HttpStatusCode.BadRequest, response.status)
            }
        }

        @Test
        fun `Returns status Unauthorized if no token is supplied`() {
            testApplication {
                val client = setupApiAndClient()
                val response = client.post("$urlVurderinger/vurderinger")

                assertEquals(HttpStatusCode.Unauthorized, response.status)
            }
        }

        @Test
        fun `returns status Forbidden if denied access to person`() {
            val forhandsvarselRequestDTO = NewVurderingRequestDTO.Forhandsvarsel(
                personident = ARBEIDSTAKER_PERSONIDENT_VEILEDER_NO_ACCESS.value,
                begrunnelse = "Fin begrunnelse",
                document = emptyList(),
                varselSvarfrist = LocalDate.now().plusDays(14),
            )
            testApplication {
                val client = setupApiAndClient()
                val response = client.post("$urlVurderinger/vurderinger") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(validToken)
                    header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT_VEILEDER_NO_ACCESS.value)
                    setBody(forhandsvarselRequestDTO)
                }

                assertEquals(HttpStatusCode.Forbidden, response.status)
            }
        }
    }

    @Nested
    @DisplayName("GET /vurderinger")
    inner class GetVurderinger {

        @Test
        fun `Successfully get vurdering (emptyList)`() {
            testApplication {
                val client = setupApiAndClient()
                val response = client.get("$urlVurderinger/vurderinger") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(validToken)
                    header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                }

                assertEquals(HttpStatusCode.NoContent, response.status)
            }
        }

        @Test
        fun `Successfully get vurdering (single vurdering)`() {
            val vurdering = generateVurdering(
                personident = Personident(ARBEIDSTAKER_PERSONIDENT.value),
                type = VurderingType.FORHANDSVARSEL,
            )
            vurderingRepository.saveManglendeMedvirkningVurdering(
                vurdering = vurdering,
                vurderingPdf = PDF_FORHANDSVARSEL,
            )

            testApplication {
                val client = setupApiAndClient()
                val response = client.get("$urlVurderinger/vurderinger") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(validToken)
                    header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                }

                assertEquals(HttpStatusCode.OK, response.status)
                val responseDTOList = response.body<List<VurderingResponseDTO>>()
                assertEquals(1, responseDTOList.size)
                val vurderingResponseDTO = responseDTOList[0]
                assertEquals(VurderingType.FORHANDSVARSEL, vurderingResponseDTO.vurderingType)
                assertEquals(ARBEIDSTAKER_PERSONIDENT.value, vurderingResponseDTO.personident)
                assertEquals(vurdering.begrunnelse, vurderingResponseDTO.begrunnelse)
            }
        }

        @Test
        fun `Successfully get vurdering (two vurderinger)`() {
            val forhandsvarsel = generateVurdering(
                personident = Personident(ARBEIDSTAKER_PERSONIDENT.value),
                type = VurderingType.FORHANDSVARSEL,
            )
            val oppfylt = generateVurdering(
                personident = Personident(ARBEIDSTAKER_PERSONIDENT.value),
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

            testApplication {
                val client = setupApiAndClient()
                val response = client.get("$urlVurderinger/vurderinger") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(validToken)
                    header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT.value)
                }

                assertEquals(HttpStatusCode.OK, response.status)
                val responseDTOList = response.body<List<VurderingResponseDTO>>()
                assertEquals(2, responseDTOList.size)
                assertEquals(VurderingType.OPPFYLT, responseDTOList[0].vurderingType)
                assertEquals(VurderingType.FORHANDSVARSEL, responseDTOList[1].vurderingType)
            }
        }

        @Test
        fun `Fails get vurdering when no access`() {
            val vurdering = generateVurdering(
                personident = Personident(ARBEIDSTAKER_PERSONIDENT_VEILEDER_NO_ACCESS.value),
                type = VurderingType.FORHANDSVARSEL,
            )
            vurderingRepository.saveManglendeMedvirkningVurdering(
                vurdering = vurdering,
                vurderingPdf = PDF_FORHANDSVARSEL,
            )

            testApplication {
                val client = setupApiAndClient()
                val response = client.get("$urlVurderinger/vurderinger") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(validToken)
                    header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_PERSONIDENT_VEILEDER_NO_ACCESS.value)
                }

                assertEquals(HttpStatusCode.Forbidden, response.status)
            }
        }
    }

    @Nested
    @DisplayName("POST /get-vurderinger")
    inner class PostGetVurderinger {
        private val personidenter = listOf(ARBEIDSTAKER_PERSONIDENT.value)
        private val requestDTO = VurderingerRequestDTO(personidenter)

        @Test
        fun `Successfully retrieves an empty group of vurderinger`() {
            testApplication {
                val client = setupApiAndClient()
                val response = client.post("$urlVurderinger/get-vurderinger") {
                    headers {
                        append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        append(HttpHeaders.Authorization, bearerHeader(validToken))
                        setBody(requestDTO)
                    }
                }

                assertEquals(HttpStatusCode.NoContent, response.status)
            }
        }

        @Test
        fun `Successfully retrieves a group of vurderinger`() {
            val forhandsvarsel = generateVurdering(
                personident = Personident(ARBEIDSTAKER_PERSONIDENT.value),
                type = VurderingType.FORHANDSVARSEL,
            )
            vurderingRepository.saveManglendeMedvirkningVurdering(
                vurdering = forhandsvarsel,
                vurderingPdf = PDF_FORHANDSVARSEL,
            )

            testApplication {
                val client = setupApiAndClient()
                val response = client.post("$urlVurderinger/get-vurderinger") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(validToken)
                    setBody(requestDTO)
                }

                assertEquals(HttpStatusCode.OK, response.status)
                val responseDTO = response.body<VurderingerResponseDTO>()
                val vurderingResponseDTO = responseDTO.vurderinger[ARBEIDSTAKER_PERSONIDENT.value]
                assertNotNull(vurderingResponseDTO)
                assertEquals(ARBEIDSTAKER_PERSONIDENT.value, vurderingResponseDTO.personident)
                assertEquals(VurderingType.FORHANDSVARSEL, vurderingResponseDTO.vurderingType)
            }
        }

        @Test
        fun `Retrieves latest vurderinger if more than one`() {
            val forhandsvarsel = generateVurdering(
                personident = Personident(ARBEIDSTAKER_PERSONIDENT.value),
                type = VurderingType.FORHANDSVARSEL,
            )
            vurderingRepository.saveManglendeMedvirkningVurdering(
                vurdering = forhandsvarsel,
                vurderingPdf = PDF_FORHANDSVARSEL,
            )
            val oppfylt = generateVurdering(
                personident = Personident(ARBEIDSTAKER_PERSONIDENT.value),
                type = VurderingType.OPPFYLT,
                createdAt = forhandsvarsel.createdAt.plusSeconds(1),
            )
            vurderingRepository.saveManglendeMedvirkningVurdering(
                vurdering = oppfylt,
                vurderingPdf = PDF_FORHANDSVARSEL,
            )

            testApplication {
                val client = setupApiAndClient()
                val response = client.post("$urlVurderinger/get-vurderinger") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(validToken)
                    setBody(requestDTO)
                }

                assertEquals(HttpStatusCode.OK, response.status)
                val responseDTO = response.body<VurderingerResponseDTO>()
                val vurderingResponseDTO = responseDTO.vurderinger[ARBEIDSTAKER_PERSONIDENT.value]
                assertNotNull(vurderingResponseDTO)
                assertEquals(ARBEIDSTAKER_PERSONIDENT.value, vurderingResponseDTO.personident)
                assertEquals(VurderingType.OPPFYLT, vurderingResponseDTO.vurderingType)
            }
        }

        @Test
        fun `Only retrieves vurderinger for which the user has access`() {
            val forhandsvarsel = generateVurdering(
                personident = Personident(ARBEIDSTAKER_PERSONIDENT.value),
                type = VurderingType.FORHANDSVARSEL,
            )
            val forhandsvarselNoAccess = generateVurdering(
                personident = Personident(ARBEIDSTAKER_PERSONIDENT_VEILEDER_NO_ACCESS.value),
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

            testApplication {
                val client = setupApiAndClient()
                val response = client.post("$urlVurderinger/get-vurderinger") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(validToken)
                    setBody(requestDTO)
                }

                assertEquals(HttpStatusCode.OK, response.status)
                val responseDTO = response.body<VurderingerResponseDTO>()
                assertTrue(responseDTO.vurderinger.keys.contains(ARBEIDSTAKER_PERSONIDENT.value))
                assertTrue(!responseDTO.vurderinger.keys.contains(ARBEIDSTAKER_PERSONIDENT_VEILEDER_NO_ACCESS.value))
            }
        }
    }
}
