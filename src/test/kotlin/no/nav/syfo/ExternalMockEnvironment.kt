package no.nav.syfo

import no.nav.syfo.common.journalforing.client.DokarkivClient
import no.nav.syfo.common.token.SystemTokenProvider
import no.nav.syfo.common.util.ClientConfig
import no.nav.syfo.infrastructure.clients.azuread.AzureAdClient
import no.nav.syfo.infrastructure.mock.dokarkivMockHttpClient
import no.nav.syfo.infrastructure.clients.pdfgen.PdfGenClient
import no.nav.syfo.infrastructure.clients.pdl.PdlClient
import no.nav.syfo.common.auth.WellKnown
import no.nav.syfo.infrastructure.database.TestDatabase
import no.nav.syfo.infrastructure.database.repository.VurderingRepository
import no.nav.syfo.infrastructure.mock.mockHttpClient
import java.nio.file.Paths

fun wellKnownInternalAzureAD(): WellKnown {
    val path = "src/test/resources/jwkset.json"
    val uri = Paths.get(path).toUri().toURL()
    return WellKnown(
        issuer = "https://sts.issuer.net/veileder/v2",
        jwksUri = uri.toString()
    )
}

class ExternalMockEnvironment private constructor() {
    val applicationState: ApplicationState = testAppState()
    val database = TestDatabase()
    val environment = testEnvironment()
    val mockHttpClient = mockHttpClient(environment = environment)
    val wellKnownInternalAzureAD = wellKnownInternalAzureAD()
    val azureAdClient = AzureAdClient(
        azureEnvironment = environment.azure,
        httpClient = mockHttpClient,
    )
    val pdlClient = PdlClient(
        azureAdClient = azureAdClient,
        pdlEnvironment = environment.clients.pdl,
        httpClient = mockHttpClient,
    )
    val dokarkivClient = DokarkivClient(
        systemTokenProvider = object : SystemTokenProvider {
            override suspend fun getSystemToken(targetClientId: String): String? = "token"
        },
        clientConfig = ClientConfig(
            baseUrl = environment.clients.dokarkiv.baseUrl,
            clientId = environment.clients.dokarkiv.clientId,
        ),
        httpClient = dokarkivMockHttpClient(),
    )
    val pdfgenClient = PdfGenClient(
        pdfGenBaseUrl = environment.clients.ispdfgen.baseUrl,
        httpClient = mockHttpClient,
    )
    val vurderingRepository = VurderingRepository(database)

    companion object {
        val instance: ExternalMockEnvironment = ExternalMockEnvironment()
    }
}
