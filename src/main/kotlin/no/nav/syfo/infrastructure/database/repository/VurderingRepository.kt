package no.nav.syfo.infrastructure.database.repository

import com.fasterxml.jackson.core.type.TypeReference
import no.nav.syfo.application.IVurderingRepository
import no.nav.syfo.domain.JournalpostId
import no.nav.syfo.domain.ManglendeMedvirkningVurdering
import no.nav.syfo.domain.Personident
import no.nav.syfo.domain.Varsel
import no.nav.syfo.domain.Veilederident
import no.nav.syfo.domain.VurderingType
import no.nav.syfo.domain.api.DocumentComponent
import no.nav.syfo.infrastructure.database.DatabaseInterface
import no.nav.syfo.infrastructure.database.toList
import no.nav.syfo.util.configuredJacksonMapper
import java.sql.Connection
import java.sql.Date
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Types
import java.time.OffsetDateTime
import java.util.UUID

class VurderingRepository(private val database: DatabaseInterface) : IVurderingRepository {

    private val mapper = configuredJacksonMapper()

    override fun saveManglendeMedvirkningVurdering(
        vurdering: ManglendeMedvirkningVurdering,
        vurderingPdf: ByteArray,
    ): ManglendeMedvirkningVurdering =
        database.connection.use { connection ->
            val pVurdering = connection.saveVurdering(vurdering)
            val pVarsel = when (vurdering) {
                is ManglendeMedvirkningVurdering.Forhandsvarsel -> connection.saveVarsel(pVurdering.id, vurdering.varsel)
                else -> null
            }
            val pVurderingPdf = connection.saveVurderingPdf(pVurdering.id, pVurdering.createdAt, vurderingPdf)
            connection.commit()

            pVurdering.toManglendeMedvirkningVurdering(pVarsel)
        }

    private fun Connection.saveVurdering(vurdering: ManglendeMedvirkningVurdering): PVurdering {
        val pVurdering = this.prepareStatement(INSERT_INTO_VURDERING).use {
            it.setString(1, vurdering.uuid.toString())
            it.setString(2, vurdering.personident.value)
            it.setObject(3, vurdering.createdAt)
            it.setObject(4, vurdering.createdAt)
            it.setString(5, vurdering.veilederident.value)
            it.setString(6, vurdering.vurderingType.name)
            it.setString(7, vurdering.begrunnelse)
            it.setObject(8, mapper.writeValueAsString(vurdering.document))
            it.setNull(9, Types.VARCHAR)
            it.setNull(10, Types.TIMESTAMP_WITH_TIMEZONE)
            it.executeQuery().toList { toPVurdering() }.firstOrNull()
        }
        if (pVurdering == null) {
            this.rollback()
            throw SQLException("Creating vurdering failed, no rows affected.")
        }
        return pVurdering
    }

    private fun Connection.saveVurderingPdf(vurderingId: Int, now: OffsetDateTime, pdf: ByteArray): PVurderingPdf {
        val pVurderingPdf = this.prepareStatement(INSERT_INTO_VURDERING_PDF).use {
            it.setString(1, UUID.randomUUID().toString())
            it.setObject(2, now)
            it.setInt(3, vurderingId)
            it.setBytes(4, pdf)
            it.executeQuery().toList { toPVurderingPdf() }.firstOrNull()
        }
        if (pVurderingPdf == null) {
            this.rollback()
            throw SQLException("Creating vurdering pdf failed, no rows affected.")
        }
        return pVurderingPdf
    }

    private fun Connection.saveVarsel(vurderingId: Int, varsel: Varsel): PVarsel {
        val pVarsel = this.prepareStatement(INSERT_INTO_VARSEL).use {
            it.setString(1, varsel.uuid.toString())
            it.setObject(2, varsel.createdAt)
            it.setObject(3, varsel.createdAt)
            it.setInt(4, vurderingId)
            it.setDate(5, Date.valueOf(varsel.svarfrist))
            it.setNull(6, Types.TIMESTAMP_WITH_TIMEZONE)
            it.executeQuery().toList { toPVarsel() }.firstOrNull()
        }
        if (pVarsel == null) {
            this.rollback()
            throw SQLException("Creating varsel failed, no rows affected.")
        }
        return pVarsel
    }

    companion object {
        private const val INSERT_INTO_VURDERING =
            """
            INSERT INTO VURDERING (
                id,
                uuid,
                personident,
                created_at,
                updated_at,
                veilederident,
                type,
                begrunnelse,
                document,
                journalpost_id,
                published_at
            ) values (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?)
            RETURNING *
            """

        private const val INSERT_INTO_VURDERING_PDF =
            """
            INSERT INTO VURDERING_PDF (
                id,
                uuid,
                created_at,
                vurdering_id,
                pdf
            ) values (DEFAULT, ?, ?, ?, ?)
            RETURNING *
            """

        private const val INSERT_INTO_VARSEL =
            """
            INSERT INTO VARSEL (
                id,
                uuid,
                created_at,
                updated_at,
                vurdering_id,
                svarfrist,
                published_at
            ) values (DEFAULT, ?, ?, ?, ?, ?, ?)
            RETURNING *
            """
    }
}

private fun ResultSet.toPVurdering(): PVurdering =
    PVurdering(
        id = getInt("id"),
        uuid = UUID.fromString(getString("uuid")),
        personident = Personident(getString("personident")),
        createdAt = getObject("created_at", OffsetDateTime::class.java),
        updatedAt = getObject("created_at", OffsetDateTime::class.java),
        veilederident = Veilederident(getString("veilederident")),
        type = VurderingType.valueOf(getString("type")),
        begrunnelse = getString("begrunnelse"),
        document = configuredJacksonMapper().readValue(
            getString("document"),
            object : TypeReference<List<DocumentComponent>>() {}
        ),
        journalpostId = getString("journalpost_id")?.let { JournalpostId(it) },
        publishedAt = getObject("published_at", OffsetDateTime::class.java),
    )

private fun ResultSet.toPVarsel(): PVarsel =
    PVarsel(
        id = getInt("id"),
        uuid = UUID.fromString(getString("uuid")),
        vurderingId = getInt("vurdering_id"),
        createdAt = getObject("created_at", OffsetDateTime::class.java),
        updatedAt = getObject("created_at", OffsetDateTime::class.java),
        svarfrist = getDate("svarfrist").toLocalDate(),
        publishedAt = getObject("published_at", OffsetDateTime::class.java),
    )

private fun ResultSet.toPVurderingPdf(): PVurderingPdf =
    PVurderingPdf(
        id = getInt("id"),
        uuid = UUID.fromString(getString("uuid")),
        vurderingId = getInt("vurdering_id"),
        createdAt = getObject("created_at", OffsetDateTime::class.java),
        pdf = getBytes("pdf"),
    )
