package com.seakernel.android.scoreapp.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.seakernel.android.scoreapp.database.entities.GameEntity

/**
 * Created by Calvin on 12/23/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
@Suppress("ClassName")
class Migration_6_7 : Migration(6, 7) {

    override fun migrate(database: SupportSQLiteDatabase) {
        addShowRoundNotesColumn(database)
    }

    private fun addShowRoundNotesColumn(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE ${GameEntity.TABLE_NAME} ADD COLUMN ${GameEntity.COLUMN_SHOW_ROUND_NOTES} INTEGER NOT NULL DEFAULT 0")
    }
}