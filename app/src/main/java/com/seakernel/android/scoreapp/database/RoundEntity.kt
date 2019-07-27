package com.seakernel.android.scoreapp.database

import androidx.room.*

/**
 * Created by Calvin on 12/23/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
@Entity(
    tableName = RoundEntity.TABLE_NAME,
    indices = [Index(name = RoundEntity.INDEX_GAME_ID, value = [RoundEntity.COLUMN_GAME_ID])],
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = [GameEntity.COLUMN_ID],
            childColumns = [RoundEntity.COLUMN_GAME_ID],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PlayerEntity::class,
            parentColumns = [PlayerEntity.COLUMN_ID],
            childColumns = [RoundEntity.COLUMN_DEALER_ID],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class RoundEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = COLUMN_ID) var id: Long,
    @ColumnInfo(name = COLUMN_GAME_ID) var gameId: Long,
    @ColumnInfo(name = COLUMN_DEALER_ID) var dealerId: Long?,
    @ColumnInfo(name = COLUMN_ROUND_NUMBER) var roundNumber: Int
) {
    companion object {
        const val TABLE_NAME = "rounds"
        const val COLUMN_ID = "uid"
        const val COLUMN_GAME_ID = "game_id"
        const val COLUMN_ROUND_NUMBER = "round_number"
        const val COLUMN_DEALER_ID = "dealer_id"
        const val INDEX_GAME_ID = "index_${TABLE_NAME}_$COLUMN_GAME_ID"
    }
}