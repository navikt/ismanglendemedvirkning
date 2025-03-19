package no.nav.syfo.infrastructure.cronjob

import no.nav.syfo.ApplicationState
import no.nav.syfo.Environment
import no.nav.syfo.application.VarselService
import no.nav.syfo.application.VurderingService
import no.nav.syfo.infrastructure.clients.leaderelection.LeaderPodClient
import no.nav.syfo.launchBackgroundTask

fun launchCronjobs(
    applicationState: ApplicationState,
    environment: Environment,
    vurderingService: VurderingService,
    varselService: VarselService,
) {
    val leaderPodClient = LeaderPodClient(
        electorPath = environment.electorPath
    )
    val cronjobRunner = CronjobRunner(
        applicationState = applicationState,
        leaderPodClient = leaderPodClient,
    )
    val cronjobs = mutableListOf<Cronjob>()

    val publishForhandsvarselCronjob = PublishForhandsvarselCronjob(varselService = varselService)
    cronjobs.add(publishForhandsvarselCronjob)

    val journalforVurderingerCronjob = JournalforVurderingerCronjob(
        vurderingService = vurderingService,
    )
    cronjobs.add(journalforVurderingerCronjob)

    if (environment.republishForhandsvarselWithAdditionalInfoCronjobEnabled) {
        val republishForhandsvarselWithAdditionalInfoCronjob = RepublishForhandsvarselWithAdditionalInfoCronjob(
            vurderingService = vurderingService,
            uuids = UUIDS,
        )
        cronjobs.add(republishForhandsvarselWithAdditionalInfoCronjob)
    }

    cronjobs.forEach {
        launchBackgroundTask(
            applicationState = applicationState,
        ) {
            cronjobRunner.start(cronjob = it)
        }
    }
}

// TOOD: Add uuids to generate new forhandsvarsel for
// Tester med to uuider fra dev, den ene har fristen gått ut, den andre ikke
val UUIDS = listOf(
    "5d3c80be-7156-43a9-9fec-4e891f79f18f",
    "78ed3c4f-c702-4aaa-b836-db4d4a72f22d"
)
