package no.nav.syfo.infrastructure.database.repository

import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class PVarsel(
    val id: Int,
    val uuid: UUID,
    val vurderingId: Int,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
    val svarfrist: LocalDate,
    val publishedAt: OffsetDateTime?,
)
