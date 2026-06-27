package com.seakernel.android.scoreapp.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.seakernel.android.scoreapp.database.entities.RoundEntity
import com.seakernel.android.scoreapp.database.entities.ScoreEntity

/**
 * Created by Calvin on 6/14/26.
 * Copyright © 2026 SeaKernel. All rights reserved.
 */
@Suppress("ClassName")
class Migration_10_11 : Migration(10, 11) {

    override fun migrate(db: SupportSQLiteDatabase) {
        addIndex(db)
    }

    private fun addIndex(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE INDEX IF NOT EXISTS `${RoundEntity.INDEX_DEALER_ID}` ON `${RoundEntity.TABLE_NAME}` (`${RoundEntity.COLUMN_DEALER_ID}`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `${ScoreEntity.INDEX_PLAYER_ID}` ON `${ScoreEntity.TABLE_NAME}` (`${ScoreEntity.COLUMN_PLAYER_ID}`)")
    }
}