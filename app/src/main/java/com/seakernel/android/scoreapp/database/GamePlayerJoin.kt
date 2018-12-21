package com.seakernel.android.scoreapp.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

/**
 * Created by Calvin on 12/21/18.
 * Copyright © 2018 SeaKernel. All rights reserved.
 */
@Entity(tableName = GamePlayerJoin.TABLE_NAME, primaryKeys = [GamePlayerJoin.COLUMN_GAME_ID, GamePlayerJoin.COLUMN_PLAYER_ID])
data class GamePlayerJoin(
    @ForeignKey(entity = GameEntity::class, parentColumns = [GameEntity.COLUMN_ID], childColumns = [COLUMN_GAME_ID], onDelete = ForeignKey.CASCADE)
    @ColumnInfo(name = COLUMN_GAME_ID, index = true) var gameId: Long,

    @ForeignKey(entity = PlayerEntity::class, parentColumns = [PlayerEntity.COLUMN_ID], childColumns = [COLUMN_PLAYER_ID], onDelete = ForeignKey.CASCADE)
    @ColumnInfo(name = COLUMN_PLAYER_ID, index = true) var playerId: Long
    ) {
    companion object {
        const val TABLE_NAME = "game_player_entity"
        const val COLUMN_GAME_ID = "gameId"
        const val COLUMN_PLAYER_ID = "playerId"
    }
}