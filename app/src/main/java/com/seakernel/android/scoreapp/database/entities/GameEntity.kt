package com.seakernel.android.scoreapp.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Created by Calvin on 12/20/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
@Entity(tableName = GameEntity.TABLE_NAME)
data class GameEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = COLUMN_ID, index = true) var uid: Long,
    @ColumnInfo(name = COLUMN_NAME) var name: String,
    @ColumnInfo(name = COLUMN_LAST_PLAYED) var date: String,
    @ColumnInfo(name = COLUMN_HAS_DEALER) var hasDealer: Boolean,
    @ColumnInfo(name = COLUMN_SHOW_ROUNDS) var showRounds: Boolean,
    @ColumnInfo(name = COLUMN_REVERSED_SCORING) var reversedScoring: Boolean,
    @ColumnInfo(name = COLUMN_MAX_SCORE) var maxScore: Double?,
    @ColumnInfo(name = COLUMN_MAX_ROUNDS) var maxRounds: Int?,
    @ColumnInfo(name = COLUMN_SHOW_ROUND_NOTES) var showRoundNotes: Boolean,
    @ColumnInfo(name = COLUMN_USE_CALCULATOR) var useCalculator: Boolean,
) {
    companion object {
        const val TABLE_NAME = "games"
        const val COLUMN_ID = "uid"
        const val COLUMN_NAME = "name"
        const val COLUMN_LAST_PLAYED = "date"
        const val COLUMN_HAS_DEALER = "hasDealer"
        const val COLUMN_SHOW_ROUNDS = "showRounds"
        const val COLUMN_REVERSED_SCORING = "reversedScoring"
        const val COLUMN_MAX_SCORE = "maxScore"
        const val COLUMN_MAX_ROUNDS = "maxRounds"
        const val COLUMN_SHOW_ROUND_NOTES = "showRoundNotes"
        const val COLUMN_USE_CALCULATOR = "useCalculator"

        const val columnNames =
            "$COLUMN_ID, $COLUMN_NAME, $COLUMN_LAST_PLAYED, $COLUMN_HAS_DEALER, $COLUMN_SHOW_ROUNDS, $COLUMN_REVERSED_SCORING, $COLUMN_MAX_SCORE, $COLUMN_MAX_ROUNDS, $COLUMN_SHOW_ROUND_NOTES, $COLUMN_USE_CALCULATOR"
    }
}