// ============================================================
// File: ReservationCacheDao.kt
// Purpose: SQLite table + queries for caching the operator's
//          recent QR verifications, so the scan history survives
//          app restarts and works offline. Read-through pattern:
//          write to cache after every successful verify-qr.
// Author: Dinil
// ============================================================
package com.smartmicrogrid.data.local

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase

data class VerifiedScan(
    val reservationId: String,
    val prosumerName: String,
    val stationName: String,
    val slotNumber: Int,
    val status: String,
    val verifiedAt: Long
)

object ReservationCacheDao {
    const val CREATE_TABLE = """
        CREATE TABLE local_verified_scans (
            reservation_id TEXT PRIMARY KEY,
            prosumer_name TEXT NOT NULL,
            station_name TEXT NOT NULL,
            slot_number INTEGER NOT NULL,
            status TEXT NOT NULL,
            verified_at INTEGER NOT NULL
        )
    """

    // Inserts or replaces a verified-scan record.
    fun upsert(db: SQLiteDatabase, v: VerifiedScan) {
        val values = ContentValues().apply {
            put("reservation_id", v.reservationId)
            put("prosumer_name", v.prosumerName)
            put("station_name", v.stationName)
            put("slot_number", v.slotNumber)
            put("status", v.status)
            put("verified_at", v.verifiedAt)
        }
        db.insertWithOnConflict("local_verified_scans", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    // Returns recent scans, newest first.
    fun recent(db: SQLiteDatabase, limit: Int = 20): List<VerifiedScan> {
        db.query(
            "local_verified_scans", null, null, null, null, null,
            "verified_at DESC", limit.toString()
        ).use { c ->
            val out = mutableListOf<VerifiedScan>()
            while (c.moveToNext()) out.add(c.toModel())
            return out
        }
    }

    private fun Cursor.toModel() = VerifiedScan(
        reservationId = getString(getColumnIndexOrThrow("reservation_id")),
        prosumerName = getString(getColumnIndexOrThrow("prosumer_name")),
        stationName = getString(getColumnIndexOrThrow("station_name")),
        slotNumber = getInt(getColumnIndexOrThrow("slot_number")),
        status = getString(getColumnIndexOrThrow("status")),
        verifiedAt = getLong(getColumnIndexOrThrow("verified_at"))
    )
}