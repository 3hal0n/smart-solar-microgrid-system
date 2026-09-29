// ============================================================
// File: StaffSessionPreferences.kt
// Purpose: Tiny SharedPreferences wrapper for a Grid Operator/staff
//          session (token/username/fullName/role), mirroring
//          OnboardingPreferences.kt's pattern. Kept separate from
//          Rukshan's ProsumerSessionDao (SQLite) since staff accounts
//          are a different login mechanism entirely (username-based,
//          no NIC) and a single-row SharedPreferences store is enough
//          - no need to pull staff sessions into the SQLite schema
//          Rukshan owns for Prosumer data.
// Author: Shalon
// ============================================================
package com.smartmicrogrid.ui.auth

import android.content.Context

private const val PREFS_NAME = "staff_session_prefs"
private const val KEY_TOKEN = "token"
private const val KEY_USERNAME = "username"
private const val KEY_FULL_NAME = "full_name"
private const val KEY_ROLE = "role"

data class StaffSession(val token: String, val username: String, val fullName: String, val role: String)

object StaffSessionPreferences {

    fun save(context: Context, session: StaffSession) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_TOKEN, session.token)
            .putString(KEY_USERNAME, session.username)
            .putString(KEY_FULL_NAME, session.fullName)
            .putString(KEY_ROLE, session.role)
            .apply()
    }

    fun read(context: Context): StaffSession? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val token = prefs.getString(KEY_TOKEN, null) ?: return null
        val username = prefs.getString(KEY_USERNAME, null) ?: return null
        val fullName = prefs.getString(KEY_FULL_NAME, null) ?: return null
        val role = prefs.getString(KEY_ROLE, null) ?: return null
        return StaffSession(token, username, fullName, role)
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
