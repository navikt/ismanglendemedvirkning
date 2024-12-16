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

    if (environment.journalforingCronjobEnabled) {
        val journalforVurderingerCronjob = JournalforVurderingerCronjob(vurderingService)
        cronjobs.add(journalforVurderingerCronjob)
    }

    cronjobs.forEach {
        launchBackgroundTask(
            applicationState = applicationState,
        ) {
            cronjobRunner.start(cronjob = it)
        }
    }
}
