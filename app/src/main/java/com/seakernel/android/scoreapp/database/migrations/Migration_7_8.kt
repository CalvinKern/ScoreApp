package com.seakernel.android.scoreapp.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.seakernel.android.scoreapp.database.entities.GameEntity

/**
 * Created by Calvin on 12/23/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
@Suppress("ClassName")
class Migration_7_8 : Migration(7, 8) {

    override fun migrate(database: SupportSQLiteDatabase) {
        addUseCalculatorColumn(database)
    }

    private fun addUseCalculatorColumn(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE ${GameEntity.TABLE_NAME} ADD COLUMN ${GameEntity.COLUMN_USE_CALCULATOR} INTEGER NOT NULL DEFAULT 1")
    }
}