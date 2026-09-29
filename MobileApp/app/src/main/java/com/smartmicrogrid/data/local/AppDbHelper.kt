// ============================================================
// File: AppDbHelper.kt
// Purpose: Shared local SQLite database helper - built jointly Day 1
//          per architecture.md §8, then frozen. onCreate() only ever
//          calls each person's own CREATE_TABLE constant, defined in
//          their own DAO file (never written inline here), so two
//          mobile developers never edit this same method body on
//          different days - see architecture.md §8's merge-conflict
//          note, which this file follows exactly. Merged 2026-09-26:
//          Shalon and Dinil each built this file independently before
//          coordinating (Shalon's DashboardCacheDao + Dinil's
//          ReservationCacheDao) - this is the reconciled version both
//          tables now go through.
// Author: Shalon + Dinil (shared infra)
// ============================================================
package com.smartmicrogrid.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

private const val DATABASE_NAME = "smart_microgrid.db"
// Bumped 1 -> 2 to add ProsumerSessionDao's table via onUpgrade for devices that already have the
// database file from before this table existed (onCreate alone only runs on a brand-new install).
private const val DATABASE_VERSION = 2

class AppDbHelper(context: Context) : SQLiteOpenHelper(
    context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION
) {

    // Creates every local table this app needs, one execSQL call per owner's own CREATE_TABLE
    // constant - add your own line here, don't edit anyone else's.
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(DashboardCacheDao.CREATE_TABLE)
        db.execSQL(ReservationCacheDao.CREATE_TABLE)
        db.execSQL(ProsumerSessionDao.CREATE_TABLE)
    }

    // Handles schema upgrades. No-op beyond Dinil's local_verified_scans rebuild for now, since
    // DATABASE_VERSION has never been bumped past 1 - extend this (not onCreate) if that changes.
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS local_verified_scans")
        onCreate(db)
    }
}
