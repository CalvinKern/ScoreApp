package com.seakernel.android.scoreapp.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.seakernel.android.scoreapp.database.*

/**
 * Created by Calvin on 12/23/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
@Suppress("ClassName")
class Migration_5_6 : Migration(5, 6) {

    override fun migrate(database: SupportSQLiteDatabase) {
        duplicateScoreTable(database)
    }

    private fun duplicateScoreTable(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE ${GameEntity.TABLE_NAME} ADD COLUMN ${GameEntity.COLUMN_SHOW_ROUNDS} INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE ${GameEntity.TABLE_NAME} ADD COLUMN ${GameEntity.COLUMN_REVERSED_SCORING} INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE ${GameEntity.TABLE_NAME} ADD COLUMN ${GameEntity.COLUMN_MAX_SCORE} REAL")
        database.execSQL("ALTER TABLE ${GameEntity.TABLE_NAME} ADD COLUMN ${GameEntity.COLUMN_MAX_ROUNDS} INTEGER")
    }
}