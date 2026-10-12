package com.seakernel.android.scoreapp.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.seakernel.android.scoreapp.database.entities.PlayerEntity

/**
 * Adds a color for each player, used for them in the graphs. Existing players have none, so they keep their seat's color.
 */
@Suppress("ClassName")
class Migration_12_13 : Migration(12, 13) {

    override fun migrate(db: SupportSQLiteDatabase) {
        if (db.hasColumn(PlayerEntity.TABLE_NAME, PlayerEntity.COLUMN_COLOR)) return
        db.execSQL("ALTER TABLE `${PlayerEntity.TABLE_NAME}` ADD COLUMN `${PlayerEntity.COLUMN_COLOR}` INTEGER")
    }
}
