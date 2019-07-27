package com.seakernel.android.scoreapp.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.seakernel.android.scoreapp.database.*

/**
 * Created by Calvin on 12/23/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
@Suppress("ClassName")
class Migration_3_4 : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        addColumnHasDealer(database)
    }

    private fun addColumnHasDealer(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE ${GameEntity.TABLE_NAME} ADD COLUMN ${GameEntity.COLUMN_HAS_DEALER} INTEGER NOT NULL DEFAULT 1")
    }
}