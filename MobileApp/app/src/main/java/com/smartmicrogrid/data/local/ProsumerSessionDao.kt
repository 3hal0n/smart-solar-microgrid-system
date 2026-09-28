// ============================================================
// File: ProsumerSessionDao.kt
// Purpose: SQLite DAO for persisting Prosumer session data locally.
//          Stores JWT token, NIC, and user info for offline access.
// Author: Rukshan
// ============================================================
package com.smartmicrogrid.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase

class ProsumerSessionDao(context: Context) {

    private val dbHelper = AppDbHelper(context)

    // Save prosumer session after successful login
    fun saveSession(nic: String, fullName: String, token: String, status: String) {
        val db = dbHelper.writableDatabase
        db.delete(TABLE_NAME, null, null) // Clear old session

        val values = ContentValues().apply {
            put(COLUMN_NIC, nic)
            put(COLUMN_FULL_NAME, fullName)
            put(COLUMN_TOKEN, token)
            put(COLUMN_STATUS, status)
            put(COLUMN_LOGGED_IN_AT, System.currentTimeMillis())
        }
        db.insert(TABLE_NAME, null, values)
    }

    // Get current active session
    fun getSession(): ProsumerSession? {
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.query(TABLE_NAME, null, null, null, null, null, null)
        return if (cursor.moveToFirst()) {
            ProsumerSession(
                nic = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NIC)),
                fullName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FULL_NAME)),
                token = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TOKEN)),
                status = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATUS)),
                loggedInAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LOGGED_IN_AT))
            )
        } else null
            .also { cursor.close() }
    }

    // Update session status (e.g., after deactivation request)
    fun updateStatus(newStatus: String) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply { put(COLUMN_STATUS, newStatus) }
        db.update(TABLE_NAME, values, null, null)
    }

    // Clear session (logout)
    fun clearSession() {
        val db = dbHelper.writableDatabase
        db.delete(TABLE_NAME, null, null)
    }

    // Check if user is logged in
    fun isLoggedIn(): Boolean = getSession() != null

    companion object {
        const val TABLE_NAME = "prosumer_session"
        const val COLUMN_NIC = "nic"
        const val COLUMN_FULL_NAME = "full_name"
        const val COLUMN_TOKEN = "token"
        const val COLUMN_STATUS = "status"
        const val COLUMN_LOGGED_IN_AT = "logged_in_at"

        // SQL statement to create the table (used by AppDbHelper)
        const val CREATE_TABLE = """
            CREATE TABLE IF NOT EXISTS $TABLE_NAME (
                $COLUMN_NIC TEXT PRIMARY KEY,
                $COLUMN_FULL_NAME TEXT NOT NULL,
                $COLUMN_TOKEN TEXT NOT NULL,
                $COLUMN_STATUS TEXT NOT NULL,
                $COLUMN_LOGGED_IN_AT INTEGER NOT NULL
            )
        """
    }
}

data class ProsumerSession(
    val nic: String, val fullName: String, val token: String, val status: String, val loggedInAt: Long
)