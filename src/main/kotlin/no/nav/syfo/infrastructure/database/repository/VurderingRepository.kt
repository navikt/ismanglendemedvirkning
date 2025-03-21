package no.nav.syfo.infrastructure.database.repository

import com.fasterxml.jackson.core.type.TypeReference
import no.nav.syfo.application.IVurderingRepository
import no.nav.syfo.domain.DocumentComponent
import no.nav.syfo.domain.JournalpostId
import no.nav.syfo.domain.Personident
import no.nav.syfo.domain.Varsel
import no.nav.syfo.domain.Veilederident
import no.nav.syfo.domain.Vurdering
import no.nav.syfo.domain.VurderingType
import no.nav.syfo.infrastructure.database.DatabaseInterface
import no.nav.syfo.infrastructure.database.toList
import no.nav.syfo.util.configuredJacksonMapper
import no.nav.syfo.util.nowUTC
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
        vurdering: Vurdering,
        vurderingPdf: ByteArray,
    ): Vurdering =
        database.connection.use { connection ->
            val pVurdering = connection.saveVurdering(vurdering)
            val pVarsel = when (vurdering) {
                is Vurdering.Forhandsvarsel -> connection.saveVarsel(pVurdering.id, vurdering.varsel)
                else -> null
            }
            connection.saveVurderingPdf(pVurdering.id, pVurdering.createdAt, vurderingPdf)
            connection.commit()

            pVurdering.toManglendeMedvirkningVurdering(pVarsel)
        }

    override fun getVurderinger(personident: Personident) =
        database.connection.use { connection ->
            connection.prepareStatement(GET_VURDERING).use {
                it.setString(1, personident.value)
                it.executeQuery().toList {
                    toPVurdering()
                }
            }.map { pVurdering ->
                val pVarsel = connection.getVarselForVurdering(pVurdering)
                pVurdering.toManglendeMedvirkningVurdering(pVarsel)
            }
        }

    override fun updatePublishedAt(vurderingUUID: UUID) =
        database.connection.use { connection ->
            connection.prepareStatement(UPDATE_PUBLISHED_AT).use {
                it.setObject(1, nowUTC())
                it.setObject(2, nowUTC())
                it.setString(3, vurderingUUID.toString())
                val updated = it.executeUpdate()
                if (updated != 1) {
                    throw SQLException("Expected a single row to be updated, got update count $updated")
                }
            }
            connection.commit()
        }

    override fun setJournalpostId(vurdering: Vurdering) = database.connection.use { connection ->
        connection.prepareStatement(UPDATE_JOURNALPOST_ID).use {
            it.setString(1, vurdering.journalpostId?.value)
            it.setObject(2, nowUTC())
            it.setString(3, vurdering.uuid.toString())
            val updated = it.executeUpdate()
            if (updated != 1) {
                throw SQLException("Expected a single row to be updated, got update count $updated")
            }
        }
        connection.commit()
    }

    override fun getNotJournalforteVurderinger(): List<Pair<Vurdering, ByteArray>> =
        database.connection.use { connection ->
            connection.prepareStatement(GET_NOT_JOURNALFORT_VURDERING).use {
                it.executeQuery().toList {
                    Pair(
                        toPVurdering(),
                        getBytes("pdf"),
                    )
                }
            }.map { (pVurdering, pdf) ->
                val pVarsel = connection.getVarselForVurdering(pVurdering)
                Pair(pVurdering.toManglendeMedvirkningVurdering(pVarsel), pdf)
            }
        }

    override fun getLatestVurderingForPersoner(
        personidenter: List<Personident>,
    ): Map<Personident, Vurdering> =
        database.connection.use { connection ->
            connection.prepareStatement(GET_VURDERINGER).use { preparedStatement ->
                preparedStatement.setString(1, personidenter.joinToString(",") { it.value })
                preparedStatement.executeQuery().toList {
                    toPVurdering().toManglendeMedvirkningVurdering(
                        if (getString("varsel_uuid") != null) {
                            PVarsel(
                                id = getInt("varsel_id"),
                                uuid = UUID.fromString(getString("varsel_uuid")),
                                createdAt = getObject("varsel_created_at", OffsetDateTime::class.java),
                                updatedAt = getObject("varsel_updated_at", OffsetDateTime::class.java),
                                vurderingId = getInt("id"),
                                publishedAt = getObject("varsel_published_at", OffsetDateTime::class.java),
                                svarfrist = getDate("varsel_svarfrist").toLocalDate(),
                            )
                        } else null
                    )
                }
            }.associateBy {
                // Den nyeste vurderingen blir valgt her siden lista er sortert med den nyeste til slutt
                it.personident
            }
        }

    override fun updatePersonident(nyPersonident: Personident, vurderinger: List<Vurdering>) = database.connection.use { connection ->
        connection.prepareStatement(UPDATE_PERSONIDENT).use {
            vurderinger.forEach { vurdering ->
                it.setString(1, nyPersonident.value)
                it.setString(2, vurdering.uuid.toString())
                val updated = it.executeUpdate()
                if (updated != 1) {
                    throw SQLException("Expected a single row to be updated, got update count $updated")
                }
            }
        }
        connection.commit()
    }

    private fun Connection.saveVurdering(vurdering: Vurdering): PVurdering {
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
            when (vurdering) {
                is Vurdering.Stans -> it.setDate(11, Date.valueOf(vurdering.stansdato))
                else -> it.setNull(11, Types.DATE)
            }
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

    private fun Connection.getVarselForVurdering(vurdering: PVurdering): PVarsel? =
        prepareStatement(GET_VARSEL_FOR_VURDERING).use {
            it.setInt(1, vurdering.id)
            it.executeQuery().toList { toPVarsel() }.firstOrNull()
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
                published_at,
                stansdato
            ) values (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?)
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

        private const val GET_VURDERING =
            """
                 SELECT v.*
                 FROM vurdering v
                 WHERE v.personident = ? 
                 ORDER BY v.created_at DESC
            """

        private const val UPDATE_PUBLISHED_AT =
            """
                UPDATE VURDERING SET updated_at=?, published_at=? WHERE uuid=?
            """

        private const val UPDATE_PERSONIDENT =
            """
                UPDATE VURDERING SET personident=? WHERE uuid=?
            """

        private const val GET_VURDERINGER =
            """
                SELECT vu.*,
                    va.id as varsel_id,
                    va.uuid as varsel_uuid,
                    va.created_at as varsel_created_at,
                    va.updated_at as varsel_updated_at,
                    va.svarfrist as varsel_svarfrist,
                    va.published_at as varsel_published_at
                FROM VURDERING vu LEFT OUTER JOIN VARSEL va ON (vu.id = va.vurdering_id) 
                WHERE vu.personident = ANY (string_to_array(?, ',')) 
                ORDER BY vu.created_at ASC
            """

        private const val UPDATE_JOURNALPOST_ID =
            """
                UPDATE VURDERING
                SET journalpost_id=?, updated_at=?
                WHERE uuid=?
            """

        private const val GET_NOT_JOURNALFORT_VURDERING =
            """
                 SELECT v.*, vpdf.pdf
                 FROM vurdering v
                 INNER JOIN vurdering_pdf vpdf ON v.id = vpdf.vurdering_id
                 WHERE v.journalpost_id IS NULL
            """

        private const val GET_VARSEL_FOR_VURDERING =
            """
                SELECT * FROM VARSEL WHERE vurdering_id = ?
            """
    }
}

internal fun ResultSet.toPVurdering(): PVurdering =
    PVurdering(
        id = getInt("id"),
        uuid = UUID.fromString(getString("uuid")),
        personident = Personident(getString("personident")),
        createdAt = getObject("created_at", OffsetDateTime::class.java),
        updatedAt = getObject("updated_at", OffsetDateTime::class.java),
        veilederident = Veilederident(getString("veilederident")),
        type = VurderingType.valueOf(getString("type")),
        begrunnelse = getString("begrunnelse"),
        stansdato = getDate("stansdato")?.toLocalDate(),
        document = configuredJacksonMapper().readValue(
            getString("document"),
            object : TypeReference<List<DocumentComponent>>() {}
        ),
        journalpostId = getString("journalpost_id")?.let { JournalpostId(it) },
        publishedAt = getObject("published_at", OffsetDateTime::class.java),
    )

fun ResultSet.toPVurderingPdf(): PVurderingPdf =
    PVurderingPdf(
        id = getInt("id"),
        uuid = UUID.fromString(getString("uuid")),
        vurderingId = getInt("vurdering_id"),
        createdAt = getObject("created_at", OffsetDateTime::class.java),
        pdf = getBytes("pdf"),
    )
