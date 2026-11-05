package no.nav.syfo.infrastructure.mock

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import no.nav.syfo.common.http.commonConfig
import no.nav.syfo.common.journalforing.JournalpostId
import no.nav.syfo.common.journalforing.dto.JournalpostResponse

const val mockedJournalpostId = 1

val dokarkivResponse = JournalpostResponse(
    journalpostId = JournalpostId(mockedJournalpostId.toString()),
    journalstatus = "status",
    journalpostferdigstilt = true,
)

fun dokarkivMockHttpClient() = HttpClient(MockEngine) {
    commonConfig()
    engine {
        addHandler {
            respond(
                content = """{"journalpostId":"$mockedJournalpostId","journalstatus":"status","journalpostferdigstilt":true}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
    }
}
