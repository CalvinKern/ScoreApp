package com.seakernel.android.scoreapp.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.seakernel.android.scoreapp.database.entities.GamePlayerJoin

/**
 * Created by Calvin on 12/23/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
@Suppress("ClassName")
class Migration_2_3 : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        addColumnPlayerPosition(database)
    }

    private fun addColumnPlayerPosition(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE ${GamePlayerJoin.TABLE_NAME} ADD COLUMN ${GamePlayerJoin.COLUMN_PLAYER_POSITION} INTEGER NOT NULL DEFAULT 0")
    }
}