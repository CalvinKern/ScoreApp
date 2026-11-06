package com.seakernel.android.scoreapp.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.seakernel.android.scoreapp.database.entities.ScoreEntity

/**
 * Adds the typed input for each score, so a calculation can be seen and edited again.
 */
@Suppress("ClassName")
class Migration_11_12 : Migration(11, 12) {

    override fun migrate(db: SupportSQLiteDatabase) {
        addScoreInputColumn(db)
    }

    private fun addScoreInputColumn(database: SupportSQLiteDatabase) {
        if (database.hasColumn(ScoreEntity.TABLE_NAME, ScoreEntity.COLUMN_SCORE_INPUT)) return
        database.execSQL("ALTER TABLE `${ScoreEntity.TABLE_NAME}` ADD COLUMN `${ScoreEntity.COLUMN_SCORE_INPUT}` TEXT")
    }
}
