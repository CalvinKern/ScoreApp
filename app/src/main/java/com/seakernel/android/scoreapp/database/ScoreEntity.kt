package com.seakernel.android.scoreapp.database

import androidx.room.*

/**
 * Created by Calvin on 12/23/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
@Entity(
    tableName = ScoreEntity.TABLE_NAME,
    indices = [Index(name = ScoreEntity.INDEX_ROUND_ID, value = [ScoreEntity.COLUMN_ROUND_ID])],
    foreignKeys = [
        ForeignKey(
            entity = PlayerEntity::class,
            parentColumns = [PlayerEntity.COLUMN_ID],
            childColumns = [ScoreEntity.COLUMN_PLAYER_ID],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = RoundEntity::class,
            parentColumns = [RoundEntity.COLUMN_ID],
            childColumns = [ScoreEntity.COLUMN_ROUND_ID],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ScoreEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = COLUMN_ID) var id: Long,
    @ColumnInfo(name = COLUMN_PLAYER_ID) var playerId: Long,
    @ColumnInfo(name = COLUMN_ROUND_ID) var roundId: Long,
    @ColumnInfo(name = COLUMN_SCORE) var score: Double,
    @ColumnInfo(name = COLUMN_SCORE_DATA) var scoreData: String
) {
    companion object {
        const val TABLE_NAME = "scores"
        const val COLUMN_ID = "uid"
        const val COLUMN_PLAYER_ID = "player_id"
        const val COLUMN_ROUND_ID = "round_id"
        const val COLUMN_SCORE = "score"
        const val COLUMN_SCORE_DATA = "score_data"
        const val INDEX_ROUND_ID = "index_${TABLE_NAME}_$COLUMN_ROUND_ID"

        const val fullyQualifiedColumns = "$TABLE_NAME.$COLUMN_ID, $TABLE_NAME.$COLUMN_PLAYER_ID, $TABLE_NAME.$COLUMN_ROUND_ID, $TABLE_NAME.$COLUMN_SCORE, $TABLE_NAME.$COLUMN_SCORE_DATA"
    }
}