package no.nav.syfo.infrastructure.clients.azuread

data class AzureEnvironment(
    val appClientId: String,
    val appClientSecret: String,
    val openidConfigTokenEndpoint: String,
)
