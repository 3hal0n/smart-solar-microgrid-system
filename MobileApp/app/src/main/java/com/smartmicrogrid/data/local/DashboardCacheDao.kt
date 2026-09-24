// ============================================================
// File: DashboardCacheDao.kt
// Purpose: Local SQLite cache for a prosumer's dashboard summary +
//          recent history, so ProsumerDashboardScreen (and any other
//          dashboard/history screen) can render instantly from the
//          last known data on open, then replace it once a fresh
//          API/fixture call resolves — instead of showing a blank
//          loading state on every visit. Owned entirely by this file
//          per architecture.md §8's merge-conflict-avoidance
//          pattern: AppDbHelper.onCreate() only calls CREATE_TABLE
//          below, never redefines it inline.
//          Reuses ui.dashboard's existing ProsumerDashboardSummary/
//          ReservationHistoryItem models rather than duplicating
//          them — a small, pragmatic cross-package reference rather
//          than introducing a separate shared-model layer this app
//          doesn't otherwise have.
// Author: Shalon
// ============================================================
package com.smartmicrogrid.data.local

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.smartmicrogrid.ui.dashboard.ProsumerDashboardSummary
import com.smartmicrogrid.ui.dashboard.ReservationHistoryItem
import org.json.JSONArray
import org.json.JSONObject

object DashboardCacheDao {

    const val TABLE_NAME = "local_dashboard_cache"
    private const val COLUMN_NIC = "nic"
    private const val COLUMN_ACTIVE_COUNT = "active_count"
    private const val COLUMN_PENDING_COUNT = "pending_count"
    private const val COLUMN_APPROVED_FUTURE_COUNT = "approved_future_count"
    private const val COLUMN_RECENT_HISTORY_JSON = "recent_history_json"
    private const val COLUMN_CACHED_AT = "cached_at"

    // One cached row per prosumer NIC — a later write for the same nic replaces the row (see
    // write()'s CONFLICT_REPLACE), rather than accumulating history.
    const val CREATE_TABLE = """
        CREATE TABLE $TABLE_NAME (
            $COLUMN_NIC TEXT PRIMARY KEY,
            $COLUMN_ACTIVE_COUNT INTEGER NOT NULL,
            $COLUMN_PENDING_COUNT INTEGER NOT NULL,
            $COLUMN_APPROVED_FUTURE_COUNT INTEGER NOT NULL,
            $COLUMN_RECENT_HISTORY_JSON TEXT NOT NULL,
            $COLUMN_CACHED_AT INTEGER NOT NULL
        )
    """

    // Reads the cached summary for a NIC, or null if nothing has been cached yet for them.
    fun read(db: SQLiteDatabase, nic: String): ProsumerDashboardSummary? {
        val cursor = db.query(TABLE_NAME, null, "$COLUMN_NIC = ?", arrayOf(nic), null, null, null)
        cursor.use {
            if (!it.moveToFirst()) {
                return null
            }
            return ProsumerDashboardSummary(
                activeCount = it.getInt(it.getColumnIndexOrThrow(COLUMN_ACTIVE_COUNT)),
                pendingCount = it.getInt(it.getColumnIndexOrThrow(COLUMN_PENDING_COUNT)),
                approvedFutureCount = it.getInt(it.getColumnIndexOrThrow(COLUMN_APPROVED_FUTURE_COUNT)),
                recentHistory = decodeHistory(it.getString(it.getColumnIndexOrThrow(COLUMN_RECENT_HISTORY_JSON))),
            )
        }
    }

    // Replaces the cached summary for a NIC with a fresh one — call this after every successful
    // refresh, so the next time this screen opens has something to render immediately.
    fun write(db: SQLiteDatabase, nic: String, summary: ProsumerDashboardSummary) {
        val values = ContentValues().apply {
            put(COLUMN_NIC, nic)
            put(COLUMN_ACTIVE_COUNT, summary.activeCount)
            put(COLUMN_PENDING_COUNT, summary.pendingCount)
            put(COLUMN_APPROVED_FUTURE_COUNT, summary.approvedFutureCount)
            put(COLUMN_RECENT_HISTORY_JSON, encodeHistory(summary.recentHistory))
            put(COLUMN_CACHED_AT, System.currentTimeMillis())
        }
        db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    // Serializes the history list to a JSON array string for storage (org.json — built into the
    // Android SDK, no new dependency needed for this).
    private fun encodeHistory(history: List<ReservationHistoryItem>): String {
        val array = JSONArray()
        history.forEach { item ->
            array.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("stationId", item.stationId)
                    put("slotId", item.slotId)
                    put("scheduledAt", item.scheduledAt)
                    put("status", item.status)
                },
            )
        }
        return array.toString()
    }

    // Deserializes a stored JSON array string back into the history list.
    private fun decodeHistory(json: String): List<ReservationHistoryItem> {
        val array = JSONArray(json)
        return (0 until array.length()).map { index ->
            val obj = array.getJSONObject(index)
            ReservationHistoryItem(
                id = obj.getString("id"),
                stationId = obj.getString("stationId"),
                slotId = obj.getString("slotId"),
                scheduledAt = obj.getString("scheduledAt"),
                status = obj.getString("status"),
            )
        }
    }
}
