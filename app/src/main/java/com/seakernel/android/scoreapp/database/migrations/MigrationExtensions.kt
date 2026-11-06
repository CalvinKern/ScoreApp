package com.seakernel.android.scoreapp.database.migrations

import androidx.sqlite.db.SupportSQLiteDatabase

/** SQLite has no ADD COLUMN IF NOT EXISTS, so migrations check first, in case one runs again */
internal fun SupportSQLiteDatabase.hasColumn(table: String, column: String): Boolean =
    query("PRAGMA table_info(`$table`)").use { cursor ->
        val nameIndex = cursor.getColumnIndexOrThrow("name")
        while (cursor.moveToNext()) {
            if (cursor.getString(nameIndex) == column) return@use true
        }
        false
    }
