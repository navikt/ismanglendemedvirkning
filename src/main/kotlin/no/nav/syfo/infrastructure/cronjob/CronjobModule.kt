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

val UUIDS = listOf(
    "b39fa390-fdd9-4750-9536-9834f709b851",
    "d3c5afd9-dd42-4c54-adef-c0b44e24cde9",
    "efbe9a3f-943a-4ca0-bbf2-51e8b844a29b",
    "4ad576d4-b69f-43c4-a76d-280101b6953c",
    "43df8eab-bd49-4f1c-ae8c-6106b21daa7a",
    "eaaa137a-5577-4658-89d7-d266a9846479",
    "662e3e8c-b355-4aca-99cc-9c62e71ccbe5",
    "5465bd48-82d4-418d-b99e-0afecf9c7528"
)
