// ============================================================
// File: AppDbHelper.kt
// Purpose: Single SQLiteOpenHelper. Each owner's CREATE_TABLE
//          constant lives in their own DAO file, called here.
//          This file is written once and essentially frozen.
// Author: Dinil (shared infra)
// ============================================================
package com.smartmicrogrid.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class AppDbHelper(context: Context) : SQLiteOpenHelper(
    context.applicationContext, "smart_microgrid.db", null, 1
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(ReservationCacheDao.CREATE_TABLE)
        // Other owners: add your CREATE_TABLE constants here.
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS local_verified_scans")
        onCreate(db)
    }
}