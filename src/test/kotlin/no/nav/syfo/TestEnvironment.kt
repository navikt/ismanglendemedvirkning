package no.nav.syfo

import no.nav.syfo.infrastructure.clients.ClientEnvironment
import no.nav.syfo.infrastructure.clients.ClientsEnvironment
import no.nav.syfo.infrastructure.clients.OpenClientEnvironment
import no.nav.syfo.infrastructure.clients.azuread.AzureEnvironment
import no.nav.syfo.infrastructure.database.DatabaseEnvironment
import no.nav.syfo.infrastructure.kafka.KafkaEnvironment

fun testEnvironment() = Environment(
    database = DatabaseEnvironment(
        host = "localhost",
        port = "5432",
        name = "ismanglendemedvirkning_dev",
        username = "username",
        password = "password",
        url = "jdbc:postgresql://localhost:5432/ismanglendemedvirkning_dev",
    ),
    kafka = KafkaEnvironment(
        aivenBootstrapServers = "kafkaBootstrapServers",
        aivenCredstorePassword = "credstorepassord",
        aivenKeystoreLocation = "keystore",
        aivenSecurityProtocol = "SSL",
        aivenTruststoreLocation = "truststore",
    ),
    azure = AzureEnvironment(
        appClientId = "ismanglendemedvirkning-client-id",
        appClientSecret = "ismanglendemedvirkning-secret",
        appWellKnownUrl = "wellknown",
        openidConfigTokenEndpoint = "azureOpenIdTokenEndpoint",
    ),
    clients = ClientsEnvironment(
        istilgangskontroll = ClientEnvironment(
            baseUrl = "isTilgangskontrollUrl",
            clientId = "dev-gcp.teamsykefravr.istilgangskontroll",
        ),
        pdl = ClientEnvironment(
            baseUrl = "pdlUrl",
            clientId = "pdlClientId",
        ),
        dokarkiv = ClientEnvironment(
            baseUrl = "dokarkivUrl",
            clientId = "dokarkivClientId",
        ),
        ispdfgen = OpenClientEnvironment(
            baseUrl = "ispdfgenUrl",
        ),
    ),
    electorPath = "electorPath",
)

fun testAppState() = ApplicationState(
    alive = true,
    ready = true,
)
