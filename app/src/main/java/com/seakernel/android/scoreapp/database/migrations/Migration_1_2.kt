package com.seakernel.android.scoreapp.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.seakernel.android.scoreapp.database.entities.GameEntity
import com.seakernel.android.scoreapp.database.entities.PlayerEntity
import com.seakernel.android.scoreapp.database.entities.RoundEntity
import com.seakernel.android.scoreapp.database.entities.ScoreEntity

/**
 * Created by Calvin on 12/23/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
@Suppress("ClassName")
class Migration_1_2 : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        createRoundTable(database)
        createScoreTable(database)
    }

    private fun createRoundTable(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE ${RoundEntity.TABLE_NAME} (" +
                "${RoundEntity.COLUMN_ID} INTEGER NOT NULL PRIMARY KEY, " +
                "${RoundEntity.COLUMN_GAME_ID} INTEGER NOT NULL, " +
                "${RoundEntity.COLUMN_DEALER_ID} INTEGER NOT NULL, " +
                "${RoundEntity.COLUMN_ROUND_NUMBER} INTEGER NOT NULL, " +
                "FOREIGN KEY (${RoundEntity.COLUMN_GAME_ID}) REFERENCES ${GameEntity.TABLE_NAME}(${GameEntity.COLUMN_ID}) ON DELETE CASCADE," +
                "FOREIGN KEY (${RoundEntity.COLUMN_DEALER_ID}) REFERENCES ${PlayerEntity.TABLE_NAME}(${PlayerEntity.COLUMN_ID}) ON DELETE CASCADE" +
                ")"
        )
        database.execSQL("CREATE INDEX ${RoundEntity.INDEX_GAME_ID} ON ${RoundEntity.TABLE_NAME}(${RoundEntity.COLUMN_GAME_ID})")
    }

    private fun createScoreTable(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE ${ScoreEntity.TABLE_NAME} (" +
                "${ScoreEntity.COLUMN_ID} INTEGER NOT NULL PRIMARY KEY, " +
                "${ScoreEntity.COLUMN_ROUND_ID} INTEGER NOT NULL, " +
                "${ScoreEntity.COLUMN_PLAYER_ID} INTEGER NOT NULL, " +
                "${ScoreEntity.COLUMN_SCORE} INTEGER NOT NULL, " +
                "${ScoreEntity.COLUMN_SCORE_DATA} TEXT NOT NULL, " +
                "FOREIGN KEY (${ScoreEntity.COLUMN_ROUND_ID}) REFERENCES ${RoundEntity.TABLE_NAME}(${RoundEntity.COLUMN_ID}) ON DELETE CASCADE," +
                "FOREIGN KEY (${ScoreEntity.COLUMN_PLAYER_ID}) REFERENCES ${PlayerEntity.TABLE_NAME}(${PlayerEntity.COLUMN_ID}) ON DELETE CASCADE" +
                ")"
        )
        database.execSQL("CREATE INDEX ${ScoreEntity.INDEX_ROUND_ID} ON ${ScoreEntity.TABLE_NAME}(${ScoreEntity.COLUMN_ROUND_ID})")
    }
}