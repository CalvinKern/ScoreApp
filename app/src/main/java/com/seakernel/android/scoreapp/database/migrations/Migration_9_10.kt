package com.seakernel.android.scoreapp.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.seakernel.android.scoreapp.database.entities.PlayerEntity

/**
 * Created by Calvin on 12/23/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
@Suppress("ClassName")
class Migration_9_10 : Migration(9, 10) {

    override fun migrate(database: SupportSQLiteDatabase) {
        addArchivedColumn(database)
    }

    private fun addArchivedColumn(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE ${PlayerEntity.TABLE_NAME} ADD COLUMN ${PlayerEntity.COLUMN_ARCHIVED} INTEGER NOT NULL DEFAULT 0")
    }
}