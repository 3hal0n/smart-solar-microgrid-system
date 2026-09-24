// ============================================================
// File: AppDbHelper.kt
// Purpose: Shared local SQLite database helper — built jointly Day 1
//          per architecture.md §8, then frozen. onCreate() only ever
//          calls each person's own CREATE_TABLE constant, defined in
//          their own DAO file (never written inline here), so two
//          mobile developers never edit this same method body on
//          different days — see architecture.md §8's merge-conflict
//          note, which this file follows exactly.
// Author: Shalon (first to build it, per confirmation 2026-09-25 that
//          nobody else was mid-edit on it; Rukshan and Migara each
//          add their own one-line db.execSQL(...) call below when
//          their own DAO exists — they don't need to touch anything
//          else in this file).
// ============================================================
package com.smartmicrogrid.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

private const val DATABASE_NAME = "smart_microgrid.db"
private const val DATABASE_VERSION = 1

class AppDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    // Creates every local table this app needs, one execSQL call per owner's own CREATE_TABLE
    // constant — add your own line here, don't edit anyone else's.
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(DashboardCacheDao.CREATE_TABLE)
        // TODO(Rukshan): add `db.execSQL(ProsumerSessionDao.CREATE_TABLE)` here once
        // ProsumerSessionDao.kt exists (architecture.md §8) — your own file, your own constant,
        // just this one extra line in this method.
        // TODO(Migara): add `db.execSQL(ReservationCacheDao.CREATE_TABLE)` here once
        // ReservationCacheDao.kt exists (architecture.md §8) — your own file, your own constant,
        // just this one extra line in this method.
    }

    // Handles schema upgrades — no-op for now, since DATABASE_VERSION has never been bumped.
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // No migrations needed yet.
    }
}
